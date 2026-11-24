package app.sidereply.service

import android.animation.ValueAnimator
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
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
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
import app.sidereply.ui.sheet.OverlayLifecycleOwner
import app.sidereply.ui.sheet.ReplySheetContent
import app.sidereply.ui.sheet.ReplySheetUiState
import app.sidereply.ui.sheet.SheetAiRound
import app.sidereply.ui.sheet.SheetPhase
import app.sidereply.ui.theme.SideReplyTheme
import app.sidereply.ui.theme.resolveSideReplyColorScheme
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.hypot

/**
 * Foreground service owning two non-accessibility Material 3 overlay windows:
 *  - a draggable Material You floating orb, visible only while the target app is foreground;
 *  - a Compose Material 3 reply bottom sheet shown after a deliberate tap.
 *
 * Foreground detection uses UsageStatsManager (authorised through Root); screenshots
 * run `/system/bin/screencap` through su. The bubble and sheet are removed before the
 * screenshot, so the assistant never captures its own UI.
 */
class FloatingAssistantService : Service() {

    private enum class ErrorStage { CONFIG, CAPTURE, VISION, AI }

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
    private var bubbleOrbView: View? = null
    private var sheetView: SheetRootLayout? = null
    private var sheetLifecycleOwner: OverlayLifecycleOwner? = null
    private var sheetUiState by mutableStateOf(ReplySheetUiState())
    private var lastErrorStage: ErrorStage = ErrorStage.AI

    private var currentSession = 0
    private var assistantBusy = false
    private var lastForegroundPackage = ""
    private var lastForegroundUser = 0
    private var lastScreenshotJpeg: ByteArray? = null
    private var lastSuggestions: List<ReplySuggestion> = emptyList()
    private var lastRequestedStyle: String = ""
    private val aiRounds = mutableListOf<SheetAiRound>()
    private var pendingRound: SheetAiRound? = null
    private val clockFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    private var lastCaptureAt = 0L
    private var lastUsageEventTimestamp = 0L
    private var lastUsagePollAt = 0L
    private var serviceReady = false

    // Dual-space (应用双开) support.
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
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH_THEME -> {
                refreshOverlayTheme()
            }
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

    private fun refreshOverlayTheme() {
        sheetUiState = sheetUiState.copy(
            themeMode = prefs.themeMode,
            dynamicColor = prefs.dynamicColor,
        )
        if (bubbleView != null) {
            hideBubble()
            if (isTargetPackage(lastForegroundPackage) && !assistantBusy) {
                showBubbleIfAllowed()
            }
        }
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
    // Material 3 Floating Orb Window
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
            bubbleOrbView = null
            bubbleParams = null
            Toast.makeText(this, R.string.toast_overlay_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildBubbleView(): View {
        val scheme = resolveSideReplyColorScheme(this, prefs.themeMode, prefs.dynamicColor)
        val container = BubbleView(this).apply {
            isClickable = true
            isFocusable = true
            contentDescription = getString(R.string.bubble_content_description)
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                startAssistantFlow()
            }
        }

        val orb = FrameLayout(this).apply {
            background = createBubbleDrawable(
                ringColor = scheme.surface.copy(alpha = 0.92f).toArgb(),
                fillColor = scheme.primary.toArgb(),
            )
            isClickable = false
            elevation = dp(6).toFloat()
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true
        }

        val glyph = ImageView(this).apply {
            setImageResource(R.drawable.ic_bubble_sparkle)
            imageTintList = ColorStateList.valueOf(scheme.onPrimary.toArgb())
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            isClickable = false
        }
        orb.addView(
            glyph,
            FrameLayout.LayoutParams(
                dp(BUBBLE_GLYPH_DP),
                dp(BUBBLE_GLYPH_DP),
                Gravity.CENTER,
            ),
        )

        bubbleOrbView = orb
        container.addView(
            orb,
            FrameLayout.LayoutParams(
                dp(BUBBLE_VISIBLE_DP),
                dp(BUBBLE_VISIBLE_DP),
                Gravity.CENTER,
            ),
        )
        return container
    }

    private fun createBubbleDrawable(ringColor: Int, fillColor: Int): LayerDrawable {
        val outerRing = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ringColor)
        }
        val innerFill = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fillColor)
        }
        return LayerDrawable(arrayOf(outerRing, innerFill)).apply {
            val inset = dp(BUBBLE_RING_DP)
            setLayerInset(1, inset, inset, inset, inset)
        }
    }

    private fun animateBubbleScale(target: Float) {
        bubbleOrbView?.animate()
            ?.scaleX(target)
            ?.scaleY(target)
            ?.setDuration(if (target < 1f) 90L else 180L)
            ?.setInterpolator(if (target < 1f) DecelerateInterpolator() else OvershootInterpolator(1.6f))
            ?.start()
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
                animateBubbleScale(0.88f)
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
                animateBubbleScale(1f)
                if (bubbleDragged) {
                    prefs.bubbleY = params.y
                    snapBubbleToEdge(view, params)
                } else {
                    view.performClick()
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                animateBubbleScale(1f)
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
            duration = 200
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
        bubbleOrbView = null
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
    // Capture + Vision AI flow
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

    private fun startVision(bitmap: Bitmap, session: Int) {
        if (ensureSheet() == null) {
            bitmap.recycle()
            failWithoutSheet()
            return
        }
        renderLoading(R.string.sheet_generating)
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
        renderLoading(R.string.sheet_generating)
        requestSuggestions(session = session, image = jpeg)
    }

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

        pendingRound = SheetAiRound(
            index = aiRounds.size + 1,
            imageKb = image.size / 1024,
            style = style,
            suggestions = emptyList(),
            time = clockFormat.format(java.util.Date()),
            pending = true,
        )
        syncRoundsState()

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
        renderLoading(R.string.sheet_generating)
        requestSuggestions(
            session = session,
            previous = previous,
            style = style,
            image = image,
        )
    }

    private fun regenerateWithStyle() {
        if (lastScreenshotJpeg == null) return
        val style = sheetUiState.styleInput
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(40)
        hideSheetKeyboard()
        retryAi(style)
    }

    private fun hideSheetKeyboard() {
        val root = sheetView ?: return
        getSystemService(InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(root.windowToken, 0)
    }

    private fun retryCapture() {
        closeSheet(restoreBubble = false)
        lastCaptureAt = 0L
        mainHandler.postDelayed({ startAssistantFlow() }, CAPTURE_DELAY_MS)
    }

    private fun failWithoutSheet() {
        assistantBusy = false
        Toast.makeText(this, R.string.sheet_overlay_failed, Toast.LENGTH_SHORT).show()
        mainHandler.postDelayed({ showBubbleIfAllowed() }, 120)
    }

    // ---------------------------------------------------------------------------------
    // Material 3 Compose Reply Sheet Overlay
    // ---------------------------------------------------------------------------------

    private fun ensureSheet(): SheetRootLayout? {
        sheetView?.let { return it }
        val bars = currentSystemBarInsets()
        sheetUiState = ReplySheetUiState(
            phase = SheetPhase.LOADING,
            loadingMessageRes = R.string.sheet_generating,
            rounds = emptyList(),
            suggestions = emptyList(),
            styleInput = "",
            themeMode = prefs.themeMode,
            dynamicColor = prefs.dynamicColor,
            topInsetPx = bars.top,
            bottomInsetPx = bars.bottom,
        )

        val owner = OverlayLifecycleOwner().apply { onCreate() }
        val root = SheetRootLayout(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            onBackPressed = { closeSheet() }
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                SideReplyTheme(
                    themeMode = sheetUiState.themeMode,
                    dynamicColor = sheetUiState.dynamicColor,
                ) {
                    ReplySheetContent(
                        state = sheetUiState,
                        onClose = { closeSheet() },
                        onCopySuggestion = { text -> copyReply(text) },
                        onStyleInputChange = { next ->
                            sheetUiState = sheetUiState.copy(styleInput = next)
                        },
                        onRegenerate = { regenerateWithStyle() },
                        onRetry = {
                            when (lastErrorStage) {
                                ErrorStage.AI -> retryAi()
                                else -> retryCapture()
                            }
                        },
                        onOpenSettings = {
                            closeSheet(restoreBubble = false)
                            openSettings()
                        },
                    )
                }
            }
        }
        root.addView(
            composeView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            setTitle("SideReplySheet")
            setFitInsetsTypes(0)
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        try {
            windowManager.addView(root, params)
            owner.onResume()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to add reply sheet", e)
            owner.onDestroy()
            return null
        }

        sheetLifecycleOwner = owner
        sheetView = root
        root.post { root.requestFocus() }
        return root
    }

    private fun closeSheet(restoreBubble: Boolean = true) {
        currentSession++
        hideSheetKeyboard()
        val root = sheetView
        val owner = sheetLifecycleOwner
        sheetView = null
        sheetLifecycleOwner = null
        lastScreenshotJpeg = null
        lastSuggestions = emptyList()
        lastRequestedStyle = ""
        aiRounds.clear()
        pendingRound = null
        assistantBusy = false

        owner?.onDestroy()
        if (root != null) {
            try {
                windowManager.removeViewImmediate(root)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to remove reply sheet", e)
            }
        }
        if (restoreBubble) {
            mainHandler.postDelayed({ showBubbleIfAllowed() }, 120)
        }
    }

    private fun renderLoading(messageRes: Int) {
        sheetUiState = sheetUiState.copy(
            phase = SheetPhase.LOADING,
            loadingMessageRes = messageRes,
            rounds = currentRoundsSnapshot(),
        )
    }

    private fun showError(message: String, stage: ErrorStage) {
        if (ensureSheet() == null) {
            failWithoutSheet()
            return
        }
        if (!assistantBusy) assistantBusy = true
        hideBubble()
        lastErrorStage = stage

        val failedRound = pendingRound
        if (failedRound != null) {
            pendingRound = null
            aiRounds += failedRound.copy(pending = false, failed = true)
        }

        sheetUiState = sheetUiState.copy(
            phase = SheetPhase.ERROR,
            errorMessage = message,
            rounds = currentRoundsSnapshot(),
            showRetry = stage != ErrorStage.CONFIG,
            showOpenSettings = stage == ErrorStage.CONFIG || stage == ErrorStage.AI,
        )
    }

    private fun renderSuggestions(suggestions: List<ReplySuggestion>) {
        val finished = (pendingRound ?: SheetAiRound(
            index = aiRounds.size + 1,
            imageKb = (lastScreenshotJpeg?.size ?: 0) / 1024,
            style = lastRequestedStyle,
            suggestions = emptyList(),
            time = clockFormat.format(java.util.Date()),
        )).copy(suggestions = suggestions, pending = false)
        pendingRound = null
        aiRounds += finished

        sheetUiState = sheetUiState.copy(
            phase = SheetPhase.SUGGESTIONS,
            suggestions = suggestions,
            rounds = currentRoundsSnapshot(),
        )
    }

    private fun syncRoundsState() {
        sheetUiState = sheetUiState.copy(rounds = currentRoundsSnapshot())
    }

    private fun currentRoundsSnapshot(): List<SheetAiRound> =
        aiRounds.toList() + listOfNotNull(pendingRound)

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

    private inner class BubbleView(context: Context) : FrameLayout(context) {
        override fun onTouchEvent(event: MotionEvent): Boolean = handleBubbleTouch(this, event)

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }
    }

    companion object {
        const val ACTION_STOP = "app.sidereply.action.STOP"
        const val ACTION_REFRESH_THEME = "app.sidereply.action.REFRESH_THEME"

        @Volatile
        var isRunning: Boolean = false
            private set

        private const val TAG = "SideReplyService"
        private const val CHANNEL_ID = "side_reply_overlay"
        private const val NOTIFICATION_ID = 5311
        private const val POLL_INTERVAL_MS = 900L
        private const val INITIAL_USAGE_LOOKBACK_MS = 10 * 60 * 1000L
        private const val RECENT_USAGE_LOOKBACK_MS = 15_000L

        private const val FOCUS_PROBE_HIDDEN_MS = 1_800L
        private const val FOCUS_PROBE_VISIBLE_MS = 5_000L
        private const val FOCUS_PROBE_FAILURE_BACKOFF_MS = 60_000L

        private val USER_INFO = Regex("""UserInfo\{(\d+):""")

        private const val MIN_CAPTURE_INTERVAL_MS = 1_100L
        private const val CAPTURE_DELAY_MS = 180L
        private const val BUBBLE_WINDOW_DP = 48
        private const val BUBBLE_VISIBLE_DP = 34
        private const val BUBBLE_GLYPH_DP = 17
        private const val BUBBLE_RING_DP = 2
    }
}
