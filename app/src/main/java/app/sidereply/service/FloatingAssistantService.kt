package app.sidereply.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import app.sidereply.MainActivity
import app.sidereply.R
import app.sidereply.ai.AiClient
import app.sidereply.ai.AiErrorMessages
import app.sidereply.ai.EndpointNormalizer
import app.sidereply.ai.ReplySuggestion
import app.sidereply.data.AppPreferences
import app.sidereply.root.RootManager
import app.sidereply.root.RootShell
import app.sidereply.root.WindowFocusProbe
import app.sidereply.ui.SheetRootLayout
import app.sidereply.ui.materialContext
import com.google.android.material.color.MaterialColors
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.hypot

/**
 * A foreground service that owns two non-accessibility overlay windows:
 *  - a tiny draggable bubble, visible only while the target app is foreground;
 *  - a reply sheet shown after a deliberate tap.
 *
 * Foreground detection uses UsageStatsManager (authorised through Root); screenshots
 * run `/system/bin/screencap` through su. The bubble and sheet are removed before the
 * screenshot, so the assistant never captures its own UI. There is no on-device OCR: the
 * compressed screenshot goes to the configured vision model, which reads the chat itself.
 */
class FloatingAssistantService : Service() {

    private enum class ErrorStage { CONFIG, CAPTURE, VISION, AI }

    /** One request/response exchange with the model, shown in the sheet's conversation log. */
    private data class AiRound(
        val index: Int,
        val imageKb: Int,
        val style: String,
        val suggestions: List<ReplySuggestion>,
        val time: String,
        val pending: Boolean = false,
        val failed: Boolean = false,
    )

    private lateinit var prefs: AppPreferences
    private lateinit var windowManager: WindowManager
    private lateinit var keyguardManager: KeyguardManager
    private lateinit var powerManager: PowerManager
    private lateinit var usageStatsManager: UsageStatsManager

    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val rootExecutor = Executors.newSingleThreadExecutor()
    private val focusExecutor = Executors.newSingleThreadExecutor()
    private val aiClient = AiClient()

    private var bubbleView: View? = null
    private var sheetView: SheetRootLayout? = null
    private var currentSession = 0
    private var assistantBusy = false
    private var lastForegroundPackage = ""
    private var lastForegroundUser = 0
    private var lastScreenshotJpeg: ByteArray? = null
    private var lastSuggestions: List<ReplySuggestion> = emptyList()
    private var lastRequestedStyle: String = ""
    /** Every finished exchange with the model in this sheet session, oldest first. */
    private val aiRounds = mutableListOf<AiRound>()
    /** The exchange currently in flight; shown in the log before the model answers. */
    private var pendingRound: AiRound? = null
    private val clockFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    private var lastCaptureAt = 0L
    private var lastUsageEventTimestamp = 0L
    private var lastUsagePollAt = 0L
    private var serviceReady = false

    // Dual-space (应用双开) support: UsageStats cannot see other users, so the focused window
    // is probed through root instead. The probe is skipped entirely when the device has no
    // second user, so phones without clones pay nothing.
    private var dualSpacePresent = false
    private var focusProbeInFlight = false
    private var nextFocusProbeAt = 0L

    // Bubble drag state.
    private var dragStartX = 0
    private var dragStartY = 0
    private var touchStartRawX = 0f
    private var touchStartRawY = 0f
    private var bubbleDragged = false
    private var bubbleParams: WindowManager.LayoutParams? = null

    private val touchSlop by lazy {
        ViewConfiguration.get(this).scaledTouchSlop
    }

    private val homePackages: Set<String> by lazy {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager
            .queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (!powerManager.isInteractive || keyguardManager.isKeyguardLocked) {
                if (sheetView != null) closeSheet(restoreBubble = false)
                hideBubble()
            } else {
                updateForegroundPackage()
                maybeProbeFocusedWindow()
                if (isTargetPackage(lastForegroundPackage) && !assistantBusy) {
                    showBubbleIfAllowed()
                } else {
                    hideBubble()
                }
            }
            mainHandler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        prefs = AppPreferences(this)
        windowManager = getSystemService(WindowManager::class.java)
        keyguardManager = getSystemService(KeyguardManager::class.java)
        powerManager = getSystemService(PowerManager::class.java)
        usageStatsManager = getSystemService(UsageStatsManager::class.java)

        createNotificationChannel()
        startInForeground()

        if (!RootManager.canDrawOverlays(this) || !RootManager.hasUsageAccess(this)) {
            Log.w(TAG, "Missing overlay or usage access; stopping root overlay service")
            Toast.makeText(this, R.string.service_permissions_missing, Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        serviceReady = true
        detectDualSpaces()
        mainHandler.post(pollRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return if (serviceReady) START_STICKY else START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        closeSheet(restoreBubble = false)
        hideBubble()
    }

    override fun onDestroy() {
        isRunning = false
        serviceReady = false
        currentSession++
        closeSheet(restoreBubble = false)
        hideBubble()
        mainHandler.removeCallbacksAndMessages(null)
        worker.shutdownNow()
        rootExecutor.shutdownNow()
        focusExecutor.shutdownNow()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------------
    // Foreground service + UsageStats polling
    // ---------------------------------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startInForeground() {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(contentIntent)
            .setColor(themeColor(androidx.appcompat.R.attr.colorPrimary))
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * UsageStats gives us the last resumed activity without reading window contents or
     * connecting to an accessibility service. Querying a short recent window is enough
     * because we poll continuously while the overlay is running.
     */
    private fun updateForegroundPackage() {
        val now = System.currentTimeMillis()
        val firstQuery = lastUsagePollAt == 0L
        val begin = if (firstQuery) {
            now - INITIAL_USAGE_LOOKBACK_MS
        } else {
            maxOf(lastUsagePollAt - 1_000L, now - RECENT_USAGE_LOOKBACK_MS)
        }

        val events = try {
            usageStatsManager.queryEvents(begin, now)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query UsageStats", e)
            return
        }

        var newestTimestamp = lastUsageEventTimestamp
        var newestPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType != UsageEvents.Event.ACTIVITY_RESUMED) continue
            if (event.timeStamp <= newestTimestamp) continue
            val pkg = event.packageName ?: continue
            newestTimestamp = event.timeStamp
            newestPackage = pkg
        }

        lastUsagePollAt = now
        if (newestPackage != null) {
            lastUsageEventTimestamp = newestTimestamp
            lastForegroundPackage = newestPackage
        }
    }

    private fun isTargetPackage(pkg: String): Boolean {
        if (pkg.isBlank() || pkg == packageName) return false
        if (pkg == "com.android.systemui" || pkg == "android") return false
        // Exact match, plus `.clone`-style package names used by some cloners, plus the
        // dual-space copy, which keeps the package name but runs as another Android user.
        if (isWechatPackage(pkg)) return true
        if (!prefs.showInAllApps) return false
        if (pkg in homePackages) return false
        return true
    }

    private fun isWechatPackage(pkg: String): Boolean =
        pkg == AppPreferences.WECHAT_PACKAGE ||
            pkg.startsWith("${AppPreferences.WECHAT_PACKAGE}.")

    // ---------------------------------------------------------------------------------
    // Dual-space (应用双开) foreground detection
    // ---------------------------------------------------------------------------------

    /**
     * `应用双开 / 应用分身` copies run either inside a container process or as another Android
     * user. UsageStats only reports events for the caller's own user, so those copies never
     * show up there and the bubble stays hidden. A second user can only be detected with root,
     * therefore the probe is only armed when one actually exists.
     */
    private fun detectDualSpaces() {
        focusExecutor.execute {
            val users = runCatching { RootShell.runText("pm list users", 8_000L) }.getOrNull()
                ?: return@execute
            val extraUsers = USER_INFO.findAll(users)
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .filter { it != 0 }
                .toSet()
            if (extraUsers.isEmpty()) {
                Log.i(TAG, "No dual-space user; focused-window probing stays off")
                return@execute
            }
            Log.i(TAG, "Dual-space users detected: $extraUsers")
            mainHandler.post { dualSpacePresent = true }
        }
    }

    /**
     * Reads the globally focused window through root, which also covers windows owned by a
     * clone user or a clone container. Throttled: the bubble is only allowed to appear after a
     * probe, so it is polled more often while hidden than while visible.
     */
    private fun maybeProbeFocusedWindow() {
        if (!dualSpacePresent || !prefs.dualAppSupport) return
        if (assistantBusy || focusProbeInFlight) return
        val now = SystemClock.uptimeMillis()
        if (now < nextFocusProbeAt) return

        focusProbeInFlight = true
        nextFocusProbeAt = now + if (bubbleView != null) FOCUS_PROBE_VISIBLE_MS else FOCUS_PROBE_HIDDEN_MS

        focusExecutor.execute {
            val window = try {
                WindowFocusProbe.parse(RootShell.runText(WindowFocusProbe.COMMAND, 4_000L))
            } catch (e: Exception) {
                Log.w(TAG, "Focused-window probe failed; retrying later", e)
                mainHandler.post {
                    focusProbeInFlight = false
                    nextFocusProbeAt = SystemClock.uptimeMillis() + FOCUS_PROBE_FAILURE_BACKOFF_MS
                }
                return@execute
            }
            mainHandler.post {
                focusProbeInFlight = false
                if (window != null && window.packageName != lastForegroundPackage) {
                    Log.i(TAG, "Focused window: ${window.packageName} (user ${window.userId})")
                }
                if (window != null) {
                    lastForegroundPackage = window.packageName
                    lastForegroundUser = window.userId
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // Bubble window
    // ---------------------------------------------------------------------------------

    private fun showBubbleIfAllowed() {
        if (!::prefs.isInitialized) return
        if (!RootManager.canDrawOverlays(this)) return
        if (assistantBusy || sheetView != null || bubbleView != null) return
        if (!powerManager.isInteractive || keyguardManager.isKeyguardLocked) return
        if (!isTargetPackage(lastForegroundPackage)) return

        val view = buildBubbleView()
        val params = buildBubbleParams()
        bubbleParams = params
        bubbleView = view

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to add floating bubble", e)
            bubbleView = null
            bubbleParams = null
            Toast.makeText(this, R.string.toast_overlay_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildBubbleView(): View {
        // The touch window stays 48dp. The visible mark is a 28dp theme-colored disc
        // with a 2dp white ring so it stays readable on top of another app.
        val ui = materialContext()
        val container = BubbleView(ui).apply {
            isClickable = true
            isFocusable = true
            contentDescription = getString(R.string.bubble_content_description)
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                startAssistantFlow()
            }
        }

        val ball = View(ui).apply {
            background = createBubbleDrawable()
            isClickable = false
            elevation = dp(6).toFloat()
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true
        }
        container.addView(
            ball,
            FrameLayout.LayoutParams(
                dp(BUBBLE_VISIBLE_DP),
                dp(BUBBLE_VISIBLE_DP),
                Gravity.CENTER,
            ),
        )
        return container
    }

    private fun createBubbleDrawable(): LayerDrawable {
        val primary = themeColor(androidx.appcompat.R.attr.colorPrimary)
        val primaryContainer = themeColor(com.google.android.material.R.attr.colorPrimaryContainer)
        // The bubble sits on WeChat, outside our page. Use the darker green so the
        // ball stays visible on a light chat, and keep the white ring.
        val fill = if (Color.luminance(primary) <= Color.luminance(primaryContainer)) {
            primary
        } else {
            primaryContainer
        }
        val whiteRing = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFFFFFFFF.toInt())
        }
        val ball = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
        }
        return LayerDrawable(arrayOf(whiteRing, ball)).apply {
            setLayerInset(
                1,
                dp(BUBBLE_RING_DP),
                dp(BUBBLE_RING_DP),
                dp(BUBBLE_RING_DP),
                dp(BUBBLE_RING_DP),
            )
        }
    }

    private fun buildBubbleParams(): WindowManager.LayoutParams {
        val metrics = windowManager.currentWindowMetrics
        val bounds = metrics.bounds
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
        )
        val size = dp(BUBBLE_WINDOW_DP)
        val maxX = (bounds.width() - size).coerceAtLeast(0)
        val minY = insets.top
        val maxY = (bounds.height() - size - insets.bottom).coerceAtLeast(minY)
        val storedY = prefs.bubbleY
        val initialY = if (storedY >= 0) storedY else (bounds.height() * 0.54f).toInt()

        return WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = maxX
            y = initialY.coerceIn(minY, maxY)
            setTitle("SideReplyBubble")
            setFitInsetsTypes(0)
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun handleBubbleTouch(view: View, event: MotionEvent): Boolean {
        val params = bubbleParams ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = params.x
                dragStartY = params.y
                touchStartRawX = event.rawX
                touchStartRawY = event.rawY
                bubbleDragged = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - touchStartRawX
                val dy = event.rawY - touchStartRawY
                if (!bubbleDragged && hypot(dx, dy) > touchSlop) {
                    bubbleDragged = true
                }
                if (bubbleDragged) {
                    val bounds = currentScreenBounds()
                    val insets = currentSystemBarInsets()
                    val size = dp(BUBBLE_WINDOW_DP)
                    params.x = (dragStartX + dx.toInt())
                        .coerceIn(0, (bounds.width() - size).coerceAtLeast(0))
                    params.y = (dragStartY + dy.toInt()).coerceIn(
                        insets.top,
                        (bounds.height() - size - insets.bottom).coerceAtLeast(insets.top),
                    )
                    safelyUpdateBubble(params)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (bubbleDragged) {
                    prefs.bubbleY = params.y
                    snapBubbleToEdge(view, params)
                } else {
                    view.performClick()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                bubbleDragged = false
                return true
            }
        }
        return false
    }

    private fun snapBubbleToEdge(view: View, params: WindowManager.LayoutParams) {
        val screenWidth = currentScreenBounds().width()
        val size = dp(BUBBLE_WINDOW_DP)
        val targetX = if (params.x + size / 2 < screenWidth / 2) {
            0
        } else {
            screenWidth - size
        }

        ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 180
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                if (bubbleView !== view || bubbleView?.isAttachedToWindow != true) {
                    animator.cancel()
                    return@addUpdateListener
                }
                params.x = animator.animatedValue as Int
                safelyUpdateBubble(params)
            }
            start()
        }
    }

    private fun safelyUpdateBubble(params: WindowManager.LayoutParams) {
        val view = bubbleView ?: return
        try {
            if (view.isAttachedToWindow) {
                windowManager.updateViewLayout(view, params)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to move floating bubble", e)
        }
    }

    private fun hideBubble() {
        val view = bubbleView ?: return
        bubbleView = null
        bubbleParams = null
        try {
            windowManager.removeViewImmediate(view)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to remove floating bubble", e)
        }
    }

    private fun currentScreenBounds() = windowManager.currentWindowMetrics.bounds

    private fun currentSystemBarInsets() =
        windowManager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
        )

    // ---------------------------------------------------------------------------------
    // Capture + OCR + AI flow
    // ---------------------------------------------------------------------------------

    private fun startAssistantFlow() {
        if (assistantBusy) return

        val problem = configurationProblem()
        if (problem != null) {
            assistantBusy = true
            currentSession++
            hideBubble()
            showError(problem, ErrorStage.CONFIG)
            return
        }

        val now = SystemClock.uptimeMillis()
        if (now - lastCaptureAt < MIN_CAPTURE_INTERVAL_MS) {
            Toast.makeText(this, R.string.sheet_too_fast, Toast.LENGTH_SHORT).show()
            return
        }
        lastCaptureAt = now

        assistantBusy = true
        currentSession++
        val session = currentSession
        lastScreenshotJpeg = null
        lastSuggestions = emptyList()
        lastRequestedStyle = ""
        aiRounds.clear()
        pendingRound = null
        hideBubble()

        // Let the window manager finish removing the bubble before grabbing pixels.
        mainHandler.postDelayed({ captureScreen(session) }, CAPTURE_DELAY_MS)
    }

    private fun configurationProblem(): String? {
        if (prefs.endpoint.isBlank() || prefs.model.isBlank()) {
            return getString(R.string.sheet_config_missing)
        }
        val key = runCatching { prefs.apiKey() }.getOrElse { "" }
        if (key.isBlank() && !EndpointNormalizer.isLocal(prefs.endpoint)) {
            return getString(R.string.sheet_no_api_key)
        }
        return null
    }

    private fun captureScreen(session: Int) {
        if (session != currentSession) return
        rootExecutor.execute {
            val result = runCatching { RootShell.capturePng() }
            mainHandler.post {
                if (session != currentSession) return@post
                result.fold(
                    onSuccess = { png -> handleRootScreenshot(png, session) },
                    onFailure = { error ->
                        Log.w(TAG, "Root screenshot failed", error)
                        val detail = error.message
                            ?.replace(Regex("\\s+"), " ")
                            ?.trim()
                            ?.take(180)
                            ?.ifBlank { null }
                            ?: getString(R.string.sheet_capture_failed)
                        showError(
                            getString(R.string.sheet_root_capture_failed, detail),
                            ErrorStage.CAPTURE,
                        )
                    },
                )
            }
        }
    }

    private fun handleRootScreenshot(png: ByteArray, session: Int) {
        val bitmap = try {
            BitmapFactory.decodeByteArray(png, 0, png.size)
        } catch (e: Throwable) {
            Log.w(TAG, "Could not decode root screenshot", e)
            null
        }
        if (bitmap == null) {
            showError(getString(R.string.sheet_capture_failed), ErrorStage.CAPTURE)
            return
        }
        if (session != currentSession) {
            bitmap.recycle()
            return
        }
        startVision(bitmap, session)
    }

    /**
     * The compressed screenshot itself goes to the model; there is no on-device OCR anymore.
     * The JPEG is kept for the session so 「换一组」 can resend it without capturing again.
     */
    private fun startVision(bitmap: Bitmap, session: Int) {
        if (ensureSheet() == null) {
            bitmap.recycle()
            failWithoutSheet()
            return
        }
        renderLoading(R.string.sheet_generating, keepImageNote = false)
        val jpeg = compressForUpload(bitmap)
        bitmap.recycle()
        if (jpeg == null) {
            showError(getString(R.string.sheet_image_failed), ErrorStage.VISION)
            return
        }
        if (session != currentSession) return
        lastScreenshotJpeg = jpeg
        lastSuggestions = emptyList()
        lastRequestedStyle = ""
        renderLoading(R.string.sheet_generating, keepImageNote = true)
        requestSuggestions(session = session, image = jpeg)
    }

    /** Downscales to a sane upload size and re-encodes as JPEG; vision models read this fine. */
    private fun compressForUpload(source: Bitmap): ByteArray? {
        val maxDimension = 1600
        val longest = maxOf(source.width, source.height)
        val scaled = if (longest > maxDimension) {
            val ratio = maxDimension.toFloat() / longest
            Bitmap.createScaledBitmap(
                source,
                (source.width * ratio).toInt().coerceAtLeast(1),
                (source.height * ratio).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            source
        }
        val output = ByteArrayOutputStream()
        val compressed = runCatching {
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, output)
        }.getOrDefault(false)
        if (scaled !== source) scaled.recycle()
        return if (compressed && output.size() > 0) output.toByteArray() else null
    }

    private fun requestSuggestions(
        session: Int,
        previous: List<ReplySuggestion> = emptyList(),
        style: String = "",
        image: ByteArray? = lastScreenshotJpeg,
    ) {
        lastRequestedStyle = style
        if (image == null) {
            showError(getString(R.string.sheet_image_failed), ErrorStage.VISION)
            return
        }
        val config = try {
            prefs.toAiConfig()
        } catch (e: Exception) {
            showError(AiErrorMessages.forThrowable(this, e), ErrorStage.AI)
            return
        }
        // Show the request in the conversation log right away, before the model answers.
        pendingRound = AiRound(
            index = aiRounds.size + 1,
            imageKb = image.size / 1024,
            style = style,
            suggestions = emptyList(),
            time = clockFormat.format(java.util.Date()),
            pending = true,
        )
        sheetView?.let { renderAiLog(it) }

        worker.execute {
            val result = runCatching {
                aiClient.suggestReplies(config, image, previous, style)
            }
            mainHandler.post {
                if (session != currentSession || sheetView == null) return@post
                result.fold(
                    onSuccess = { suggestions ->
                        lastSuggestions = suggestions
                        renderSuggestions(suggestions)
                    },
                    onFailure = { error ->
                        showError(
                            AiErrorMessages.forThrowable(this, error),
                            ErrorStage.AI,
                        )
                    },
                )
            }
        }
    }

    private fun retryAi(styleOverride: String? = null) {
        val image = lastScreenshotJpeg
        if (image == null) {
            retryCapture()
            return
        }
        currentSession++
        val session = currentSession
        val previous = lastSuggestions
        val style = styleOverride ?: lastRequestedStyle
        renderLoading(R.string.sheet_generating, keepImageNote = true)
        requestSuggestions(
            session = session,
            previous = previous,
            style = style,
            image = image,
        )
    }

    private fun regenerateWithStyle() {
        if (lastScreenshotJpeg == null) return
        val style = readStyleInput()
        hideSheetKeyboard()
        retryAi(style)
    }

    private fun readStyleInput(): String =
        sheetView
            ?.findViewById<EditText>(R.id.input_style)
            ?.text
            ?.toString()
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.take(40)
            .orEmpty()

    private fun hideSheetKeyboard() {
        val input = sheetView?.findViewById<EditText>(R.id.input_style) ?: return
        getSystemService(InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(input.windowToken, 0)
        input.clearFocus()
    }

    private fun retryCapture() {
        closeSheet(restoreBubble = false)
        // This is an explicit retry, so do not let the ordinary double-tap guard bounce it.
        lastCaptureAt = 0L
        mainHandler.postDelayed({ startAssistantFlow() }, CAPTURE_DELAY_MS)
    }

    private fun failWithoutSheet() {
        assistantBusy = false
        Toast.makeText(this, R.string.sheet_overlay_failed, Toast.LENGTH_SHORT).show()
        mainHandler.postDelayed({ showBubbleIfAllowed() }, 120)
    }

    // ---------------------------------------------------------------------------------
    // Reply sheet
    // ---------------------------------------------------------------------------------

    @SuppressLint("InflateParams")
    private fun ensureSheet(): SheetRootLayout? {
        sheetView?.let { return it }
        val root = try {
            LayoutInflater.from(materialContext())
                .inflate(R.layout.view_reply_sheet, null) as SheetRootLayout
        } catch (e: Exception) {
            Log.w(TAG, "Could not inflate reply sheet", e)
            return null
        }

        val bars = currentSystemBarInsets()
        root.setPadding(
            dp(16),
            bars.top + dp(8),
            dp(16),
            bars.bottom + dp(16),
        )
        root.onBackPressed = { closeSheet() }
        root.setOnClickListener { closeSheet() }
        root.findViewById<View>(R.id.sheet_card).setOnClickListener { /* consume */ }
        root.findViewById<View>(R.id.btn_close).setOnClickListener { closeSheet() }
        root.findViewById<View>(R.id.btn_open_settings).setOnClickListener {
            closeSheet(restoreBubble = false)
            openSettings()
        }
        root.findViewById<EditText>(R.id.input_style).setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                regenerateWithStyle()
                true
            } else {
                false
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            dimAmount = 0.32f
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            setTitle("SideReplySheet")
            setFitInsetsTypes(0)
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        try {
            windowManager.addView(root, params)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to add reply sheet", e)
            return null
        }

        sheetView = root
        root.post { root.requestFocus() }
        val card = root.findViewById<View>(R.id.sheet_card)
        root.alpha = 0f
        card.translationY = dp(22).toFloat()
        root.animate().alpha(1f).setDuration(130).start()
        card.animate()
            .translationY(0f)
            .setDuration(210)
            .setInterpolator(DecelerateInterpolator())
            .start()
        return root
    }

    private fun closeSheet(restoreBubble: Boolean = true) {
        currentSession++
        hideSheetKeyboard()
        val root = sheetView
        sheetView = null
        lastScreenshotJpeg = null
        lastSuggestions = emptyList()
        lastRequestedStyle = ""
        aiRounds.clear()
        pendingRound = null
        assistantBusy = false
        if (root != null) {
            try {
                root.animate().cancel()
                windowManager.removeViewImmediate(root)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to remove reply sheet", e)
            }
        }
        if (restoreBubble) {
            mainHandler.postDelayed({ showBubbleIfAllowed() }, 120)
        }
    }

    private fun renderLoading(
        messageRes: Int,
        keepImageNote: Boolean,
    ) {
        val root = sheetView ?: return
        root.findViewById<View>(R.id.group_error).visibility = View.GONE
        root.findViewById<View>(R.id.group_suggestions).visibility = View.GONE
        root.findViewById<View>(R.id.group_loading).visibility = View.VISIBLE
        root.findViewById<TextView>(R.id.tv_loading).setText(messageRes)
        if (!keepImageNote) {
            root.findViewById<View>(R.id.group_note).visibility = View.GONE
        }
        renderAiLog(root)
    }

    private fun showError(message: String, stage: ErrorStage) {
        val root = ensureSheet()
        if (root == null) {
            failWithoutSheet()
            return
        }
        if (!assistantBusy) assistantBusy = true
        hideBubble()

        root.findViewById<View>(R.id.group_loading).visibility = View.GONE
        root.findViewById<View>(R.id.group_suggestions).visibility = View.GONE
        root.findViewById<View>(R.id.group_error).visibility = View.VISIBLE
        root.findViewById<TextView>(R.id.tv_error).text = message

        // The exchange that just failed stays in the log, marked as such, so the record reads
        // like a real conversation instead of silently dropping the request.
        val failedRound = pendingRound
        if (failedRound != null) {
            pendingRound = null
            aiRounds += failedRound.copy(pending = false, failed = true)
        }
        renderAiLog(root)

        val retryButton = root.findViewById<TextView>(R.id.btn_retry)
        val settingsButton = root.findViewById<TextView>(R.id.btn_open_settings)
        if (stage == ErrorStage.CONFIG) {
            retryButton.visibility = View.GONE
            settingsButton.visibility = View.VISIBLE
        } else {
            retryButton.visibility = View.VISIBLE
            retryButton.setText(R.string.sheet_retry)
            retryButton.setOnClickListener {
                when (stage) {
                    ErrorStage.AI -> retryAi()
                    else -> retryCapture()
                }
            }
            settingsButton.visibility =
                if (stage == ErrorStage.AI) View.VISIBLE else View.GONE
        }
    }

    private fun renderSuggestions(suggestions: List<ReplySuggestion>) {
        val root = sheetView ?: return
        root.findViewById<View>(R.id.group_loading).visibility = View.GONE
        root.findViewById<View>(R.id.group_error).visibility = View.GONE
        root.findViewById<View>(R.id.group_suggestions).visibility = View.VISIBLE

        val finished = (pendingRound ?: AiRound(
            index = aiRounds.size + 1,
            imageKb = (lastScreenshotJpeg?.size ?: 0) / 1024,
            style = lastRequestedStyle,
            suggestions = emptyList(),
            time = clockFormat.format(java.util.Date()),
        )).copy(suggestions = suggestions, pending = false)
        pendingRound = null
        aiRounds += finished
        renderAiLog(root)

        val list = root.findViewById<LinearLayout>(R.id.list_suggestions)
        list.removeAllViews()
        suggestions.take(3).forEach { suggestion ->
            val item = LayoutInflater.from(root.context)
                .inflate(R.layout.item_reply_suggestion, list, false)
            item.findViewById<TextView>(R.id.tv_style).text = suggestion.style
            item.findViewById<TextView>(R.id.tv_text).text = suggestion.text
            val copyButton = item.findViewById<TextView>(R.id.btn_copy)
            val copy = {
                copyReply(suggestion.text)
                copyButton.setText(R.string.sheet_copied)
                mainHandler.postDelayed({
                    if (copyButton.isAttachedToWindow) {
                        copyButton.setText(R.string.sheet_copy)
                    }
                }, 1400)
            }
            item.setOnClickListener { copy() }
            copyButton.setOnClickListener { copy() }
            list.addView(item)
        }

        root.findViewById<View>(R.id.btn_regenerate).setOnClickListener {
            regenerateWithStyle()
        }
    }

    /**
     * Draws the exchange with the model as a chat log: the request appears as soon as it is
     * sent, the answer replaces the pending line. Old rounds keep their full text; the newest
     * one points at the suggestion cards below, which are the same three replies plus copy
     * buttons.
     */
    private fun renderAiLog(root: SheetRootLayout) {
        val group = root.findViewById<View>(R.id.group_note)
        val list = root.findViewById<LinearLayout>(R.id.list_ai_rounds)
        val title = root.findViewById<TextView>(R.id.tv_note_title)
        list.removeAllViews()

        val rounds = aiRounds + listOfNotNull(pendingRound)
        if (rounds.isEmpty()) {
            group.visibility = View.GONE
            return
        }
        group.visibility = View.VISIBLE
        list.visibility = View.VISIBLE
        title.text = getString(R.string.sheet_ai_log_title, rounds.last().index)
        rounds.forEachIndexed { index, round ->
            val isNewest = index == rounds.lastIndex && !round.pending && !round.failed
            list.addView(buildRoundView(list, round, isNewest))
        }
    }

    private fun buildRoundView(parent: LinearLayout, round: AiRound, isNewest: Boolean): View {
        val item = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_round, parent, false)
        item.findViewById<TextView>(R.id.tv_round_meta).text =
            getString(R.string.sheet_round_meta, round.index, round.time)

        val userText = buildString {
            append(getString(R.string.sheet_round_user, round.imageKb))
            if (round.style.isNotBlank()) {
                append(" · ").append(getString(R.string.sheet_round_style, round.style))
            }
        }
        item.findViewById<TextView>(R.id.tv_round_user).text = userText

        val aiText = when {
            round.pending -> getString(R.string.sheet_round_pending)
            round.failed -> getString(R.string.sheet_round_failed)
            isNewest -> getString(R.string.sheet_round_done)
            else -> round.suggestions.take(3).joinToString("\n") { "${it.style}：${it.text}" }
        }
        val aiView = item.findViewById<TextView>(R.id.tv_round_ai)
        aiView.text = aiText
        val aiColor = when {
            round.failed -> MaterialColors.getColor(
                aiView,
                androidx.appcompat.R.attr.colorError,
            )
            round.pending -> MaterialColors.getColor(
                aiView,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
            )
            else -> MaterialColors.getColor(
                aiView,
                com.google.android.material.R.attr.colorOnSurface,
            )
        }
        aiView.setTextColor(aiColor)
        return item
    }

    private fun themeColor(attr: Int): Int =
        MaterialColors.getColor(materialContext(), attr, 0xFF096B5A.toInt())

    private fun copyReply(text: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("AI 回复建议", text))
        Toast.makeText(this, R.string.sheet_copied_toast, Toast.LENGTH_SHORT).show()
    }

    private fun openSettings() {
        try {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
                ),
            )
        } catch (e: Exception) {
            Log.w(TAG, "Unable to open settings", e)
            Toast.makeText(this, R.string.settings_open_failed, Toast.LENGTH_LONG).show()
        }
    }

    // ---------------------------------------------------------------------------------
    // Misc
    // ---------------------------------------------------------------------------------

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /**
     * Touch handling is custom (drag + tap), so this view owns its gesture logic and
     * still routes taps through performClick for accessibility services.
     */
    private inner class BubbleView(context: Context) : FrameLayout(context) {
        override fun onTouchEvent(event: MotionEvent): Boolean = handleBubbleTouch(this, event)

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }
    }

    companion object {
        const val ACTION_STOP = "app.sidereply.action.STOP"

        @Volatile
        var isRunning: Boolean = false
            private set

        private const val TAG = "SideReplyService"
        private const val CHANNEL_ID = "side_reply_overlay"
        private const val NOTIFICATION_ID = 5311
        private const val POLL_INTERVAL_MS = 900L
        private const val INITIAL_USAGE_LOOKBACK_MS = 10 * 60 * 1000L
        private const val RECENT_USAGE_LOOKBACK_MS = 15_000L

        /** Focused-window probing: cheap enough to run every couple of seconds. */
        private const val FOCUS_PROBE_HIDDEN_MS = 1_800L
        private const val FOCUS_PROBE_VISIBLE_MS = 5_000L
        private const val FOCUS_PROBE_FAILURE_BACKOFF_MS = 60_000L

        /** `pm list users` prints one `UserInfo{0:owner:13}` per user. */
        private val USER_INFO = Regex("""UserInfo\{(\d+):""")

        private const val MIN_CAPTURE_INTERVAL_MS = 1_100L
        private const val CAPTURE_DELAY_MS = 180L
        private const val BUBBLE_WINDOW_DP = 48
        private const val BUBBLE_VISIBLE_DP = 28
        private const val BUBBLE_RING_DP = 2
    }
}
