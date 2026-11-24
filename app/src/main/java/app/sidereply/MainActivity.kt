package app.sidereply

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.WindowInsets
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.sidereply.ai.AiClient
import app.sidereply.ai.AiConfig
import app.sidereply.ai.AiErrorMessages
import app.sidereply.ai.EndpointNormalizer
import app.sidereply.data.AppPreferences
import app.sidereply.root.RootManager
import app.sidereply.service.FloatingAssistantService
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private enum class ButtonRole { FILLED, TONAL, OUTLINED }

    private lateinit var prefs: AppPreferences
    private lateinit var statusPill: Chip
    private lateinit var serviceTitle: TextView
    private lateinit var serviceState: TextView
    private lateinit var serviceDesc: TextView
    private lateinit var restrictedHint: TextView
    private lateinit var serviceButton: MaterialButton
    private lateinit var endpointInput: EditText
    private lateinit var modelInput: EditText
    private lateinit var apiKeyLayout: TextInputLayout
    private lateinit var apiKeyInput: EditText
    private lateinit var keyHint: TextView
    private lateinit var showKeyButton: TextView
    private lateinit var clearKeyButton: TextView
    private lateinit var saveButton: TextView
    private lateinit var testButton: TextView
    private lateinit var testResult: TextView
    private lateinit var wechatOnlySwitch: MaterialSwitch
    private lateinit var wechatOnlyCard: View
    private lateinit var dualAppSwitch: MaterialSwitch
    private lateinit var dualAppCard: View

    private val worker = Executors.newSingleThreadExecutor()
    private var keyVisible = false
    private var testing = false
    private var rootStarting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)

        setContentView(R.layout.activity_main)
        configureEdgeToEdge()
        applyWindowInsets()

        bindViews()
        loadValues()
        installListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshRootServiceStatus()
        updateKeyHint()
    }

    override fun onPause() {
        if (keyVisible) {
            setKeyVisible(false)
        }
        super.onPause()
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun bindViews() {
        statusPill = findViewById(R.id.tv_status_pill)
        serviceTitle = findViewById(R.id.tv_service_title)
        serviceState = findViewById(R.id.tv_service_state)
        serviceDesc = findViewById(R.id.tv_service_desc)
        restrictedHint = findViewById(R.id.tv_restricted_hint)
        serviceButton = findViewById(R.id.btn_service)
        endpointInput = findViewById(R.id.input_endpoint)
        modelInput = findViewById(R.id.input_model)
        apiKeyLayout = findViewById(R.id.input_api_key_layout)
        apiKeyInput = findViewById(R.id.input_api_key)
        keyHint = findViewById(R.id.tv_key_hint)
        showKeyButton = findViewById(R.id.btn_show_key)
        clearKeyButton = findViewById(R.id.btn_clear_key)
        saveButton = findViewById(R.id.btn_save)
        testButton = findViewById(R.id.btn_test)
        testResult = findViewById(R.id.tv_test_result)
        wechatOnlySwitch = findViewById(R.id.switch_wechat_only)
        wechatOnlyCard = findViewById(R.id.card_wechat_only)
        dualAppSwitch = findViewById(R.id.switch_dual_app)
        dualAppCard = findViewById(R.id.card_dual_app)
    }

    private fun loadValues() {
        endpointInput.setText(prefs.endpoint)
        modelInput.setText(prefs.model)
        apiKeyInput.setText("")
        wechatOnlySwitch.isChecked = !prefs.showInAllApps
        dualAppSwitch.isChecked = prefs.dualAppSupport
        updateKeyHint()
    }

    private fun installListeners() {
        serviceButton.setOnClickListener {
            if (FloatingAssistantService.isRunning) {
                stopRootOverlay()
            } else {
                startRootOverlay()
            }
        }

        saveButton.setOnClickListener { saveSettings() }
        testButton.setOnClickListener { testConnection() }

        showKeyButton.setOnClickListener {
            if (keyVisible) {
                setKeyVisible(false)
            } else if (revealSavedKeyIfNeeded()) {
                setKeyVisible(true)
            }
        }

        clearKeyButton.setOnClickListener {
            confirmClearKey()
        }

        wechatOnlySwitch.setOnCheckedChangeListener { _, checked ->
            prefs.showInAllApps = !checked
        }

        wechatOnlyCard.setOnClickListener {
            wechatOnlySwitch.toggle()
        }

        dualAppSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.dualAppSupport = checked
        }

        dualAppCard.setOnClickListener {
            dualAppSwitch.toggle()
        }
    }

    private fun configureEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.isNavigationBarContrastEnforced = false
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
    }

    private fun applyWindowInsets() {
        val scroll = findViewById<ScrollView>(R.id.root_scroll)
        val baseLeft = scroll.paddingLeft
        val baseTop = scroll.paddingTop
        val baseRight = scroll.paddingRight
        val baseBottom = scroll.paddingBottom
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            val ime = insets.getInsets(WindowInsets.Type.ime())
            view.setPadding(
                baseLeft,
                baseTop + bars.top,
                baseRight,
                baseBottom + maxOf(bars.bottom, ime.bottom),
            )
            insets
        }
        scroll.requestApplyInsets()
    }

    private fun refreshRootServiceStatus() {
        val overlayGranted = RootManager.canDrawOverlays(this)
        val usageGranted = RootManager.hasUsageAccess(this)
        renderRootServiceState(FloatingAssistantService.isRunning, overlayGranted, usageGranted)
    }

    private fun renderRootServiceState(
        running: Boolean,
        overlayGranted: Boolean,
        usageGranted: Boolean,
    ) {
        serviceButton.isEnabled = !rootStarting

        if (rootStarting) {
            statusPill.setText(R.string.status_configuring)
            styleStatus(
                com.google.android.material.R.attr.colorSecondaryContainer,
                com.google.android.material.R.attr.colorOnSecondaryContainer,
            )
            serviceTitle.setText(R.string.root_starting)
            serviceState.setText(R.string.root_waiting_for_grant)
            serviceState.setTextColor(colorAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
            serviceDesc.setText(R.string.service_off_desc)
            restrictedHint.visibility = View.VISIBLE
            serviceButton.setText(R.string.root_starting)
            applyButtonRole(serviceButton, ButtonRole.TONAL)
            return
        }

        if (running) {
            statusPill.setText(R.string.status_running)
            styleStatus(
                androidx.appcompat.R.attr.colorPrimary,
                com.google.android.material.R.attr.colorOnPrimary,
                com.google.android.material.R.attr.colorPrimaryFixedDim,
            )
            serviceTitle.setText(R.string.service_on_title)
            serviceState.setText(R.string.service_state_running)
            serviceState.setTextColor(colorAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
            serviceDesc.setText(R.string.service_on_desc)
            restrictedHint.visibility = View.GONE
            serviceButton.setText(R.string.overlay_stop)
            applyButtonRole(serviceButton, ButtonRole.OUTLINED)
            return
        }

        statusPill.setText(R.string.status_stopped)
        styleStatus(
            com.google.android.material.R.attr.colorSurfaceContainerHigh,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
        )
        serviceTitle.setText(R.string.service_off_title)
        val state = when {
            !overlayGranted && !usageGranted -> getString(R.string.service_state_needs_root)
            !overlayGranted -> getString(R.string.service_permission_overlay)
            !usageGranted -> getString(R.string.service_permission_usage)
            else -> getString(R.string.service_state_ready)
        }
        serviceState.text = state
        serviceState.setTextColor(colorAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
        serviceDesc.setText(R.string.service_off_desc)
        restrictedHint.visibility = View.VISIBLE
        serviceButton.setText(R.string.overlay_start)
        applyButtonRole(serviceButton, ButtonRole.FILLED)
    }

    private fun startRootOverlay() {
        if (rootStarting) return
        rootStarting = true
        refreshRootServiceStatus()

        val appContext = applicationContext
        worker.execute {
            val result = if (RootManager.isReady(appContext)) {
                Result.success(Unit)
            } else {
                runCatching { RootManager.grantPermissionsViaRoot(appContext) }
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                rootStarting = false
                result.fold(
                    onSuccess = {
                        try {
                            startForegroundService(
                                Intent(this, FloatingAssistantService::class.java),
                            )
                            requestNotificationPermissionIfNeeded()
                            Toast.makeText(
                                this,
                                R.string.toast_overlay_started,
                                Toast.LENGTH_SHORT,
                            ).show()
                            window.decorView.postDelayed({
                                if (!isFinishing && !isDestroyed) refreshRootServiceStatus()
                            }, 450)
                        } catch (e: Exception) {
                            refreshRootServiceStatus()
                            Toast.makeText(
                                this,
                                getString(
                                    R.string.root_error_start,
                                    e.message ?: getString(R.string.root_error_command),
                                ),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                    onFailure = { error ->
                        refreshRootServiceStatus()
                        showRootErrorDialog(error)
                    },
                )
            }
        }
    }

    private fun stopRootOverlay() {
        try {
            stopService(Intent(this, FloatingAssistantService::class.java))
            Toast.makeText(this, R.string.toast_overlay_stopped, Toast.LENGTH_SHORT).show()
            window.decorView.postDelayed({
                if (!isFinishing && !isDestroyed) refreshRootServiceStatus()
            }, 300)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(
                    R.string.root_error_start,
                    e.message ?: getString(R.string.root_error_command),
                ),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS,
            )
        }
    }

    private fun showRootErrorDialog(error: Throwable) {
        val message = rootSetupErrorMessage(error)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.root_error_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun rootSetupErrorMessage(error: Throwable): String {
        val setupError = (error as? RootManager.SetupException)?.error
        return when (setupError) {
            RootManager.SetupError.SU_NOT_FOUND -> getString(R.string.root_error_su_missing)
            RootManager.SetupError.ROOT_DENIED -> getString(R.string.root_error_denied)
            RootManager.SetupError.ROOT_TIMEOUT -> getString(R.string.root_error_timeout)
            RootManager.SetupError.OVERLAY_NOT_GRANTED -> getString(R.string.root_error_overlay)
            RootManager.SetupError.USAGE_NOT_GRANTED -> getString(R.string.root_error_usage)
            RootManager.SetupError.COMMAND_FAILED, null -> getString(R.string.root_error_command)
        }
    }

    private fun saveSettings() {
        val form = validateForm() ?: return
        hideKeyboard()

        val typedKey = apiKeyInput.text.toString().trim()
        var savedNewKey = false
        if (typedKey.isNotEmpty()) {
            try {
                prefs.saveApiKey(typedKey)
            } catch (e: Exception) {
                showInlineMessage(getString(R.string.key_save_failed), isError = true)
                return
            }
            // Keep the value in the field as a masked confirmation instead of making it
            // look like the key vanished. It is still encrypted at rest.
            apiKeyInput.setText(typedKey)
            apiKeyInput.setSelection(typedKey.length)
            setKeyVisible(false)
            savedNewKey = true
        }

        prefs.endpoint = form.first
        prefs.model = form.second
        endpointInput.setText(form.first)
        modelInput.setText(form.second)
        updateKeyHint()

        val hasKey = readSavedKey().isNotBlank()
        val message = if (hasKey || EndpointNormalizer.isLocal(form.first)) {
            getString(R.string.saved)
        } else {
            getString(R.string.saved_without_key)
        }
        showInlineMessage(message, isError = false)
        if (savedNewKey) {
            Toast.makeText(this, R.string.key_saved_toast, Toast.LENGTH_SHORT).show()
        }
    }

    private fun testConnection() {
        if (testing) return
        val form = validateForm() ?: return
        hideKeyboard()

        val key = apiKeyInput.text.toString().trim().ifBlank { prefs.apiKey() }
        val config = AiConfig(endpoint = form.first, model = form.second, apiKey = key)

        testing = true
        testButton.isEnabled = false
        testButton.setText(R.string.test_testing)
        showInlineMessage(getString(R.string.test_testing), isError = false)

        worker.execute {
            val result = runCatching { AiClient().testConnection(config) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                testing = false
                testButton.isEnabled = true
                testButton.setText(R.string.test_connection)

                result.fold(
                    onSuccess = { raw ->
                        val summary = raw
                            .replace(Regex("\\s+"), " ")
                            .trim()
                            .take(48)
                        val message = if (summary.isBlank()) {
                            getString(R.string.test_success_empty)
                        } else {
                            getString(R.string.test_success, summary)
                        }
                        showInlineMessage(message, isError = false)
                    },
                    onFailure = { error ->
                        showInlineMessage(
                            AiErrorMessages.forThrowable(this, error),
                            isError = true,
                        )
                    },
                )
            }
        }
    }

    private fun validateForm(): Pair<String, String>? {
        val rawEndpoint = endpointInput.text.toString().trim()
        val model = modelInput.text.toString().trim()

        if (rawEndpoint.isEmpty()) {
            showInlineMessage(getString(R.string.endpoint_required), isError = true)
            endpointInput.requestFocus()
            return null
        }

        val endpoint = try {
            EndpointNormalizer.normalize(rawEndpoint)
        } catch (e: EndpointNormalizer.InvalidEndpointException) {
            val message = when (e.error) {
                EndpointNormalizer.Error.CLEARTEXT_NOT_ALLOWED ->
                    getString(R.string.ai_error_cleartext)
                else -> getString(R.string.endpoint_invalid)
            }
            showInlineMessage(message, isError = true)
            endpointInput.requestFocus()
            return null
        }
        if (endpoint != rawEndpoint) {
            endpointInput.setText(endpoint)
            endpointInput.setSelection(endpoint.length)
        }

        if (model.isEmpty()) {
            showInlineMessage(getString(R.string.model_required), isError = true)
            modelInput.requestFocus()
            return null
        }
        return endpoint to model
    }

    private fun updateKeyHint() {
        val storedKey = readSavedKey()
        val hasKey = storedKey.isNotBlank()
        if (hasKey) {
            keyHint.text = getString(R.string.api_key_saved_hint, keyTail(storedKey))
            apiKeyLayout.placeholderText = getString(R.string.api_key_saved_placeholder)
        } else {
            keyHint.setText(R.string.api_key_empty_hint)
            apiKeyLayout.placeholderText = getString(R.string.hint_api_key)
        }
        clearKeyButton.visibility = if (hasKey) View.VISIBLE else View.GONE
    }

    /**
     * When the user types or pastes a key it is already in the field. On a fresh launch the
     * field starts empty, so "显示" explicitly decrypts the saved key on demand.
     */
    private fun revealSavedKeyIfNeeded(): Boolean {
        if (!apiKeyInput.text.isNullOrEmpty()) return true
        val storedKey = readSavedKey()
        if (storedKey.isEmpty()) {
            Toast.makeText(this, R.string.key_nothing_to_show, Toast.LENGTH_SHORT).show()
            return false
        }
        apiKeyInput.setText(storedKey)
        apiKeyInput.setSelection(storedKey.length)
        return true
    }

    private fun readSavedKey(): String =
        runCatching { prefs.apiKey() }.getOrNull().orEmpty()

    private fun keyTail(key: String): String =
        if (key.length >= 8) key.takeLast(4) else "••••"

    private fun setKeyVisible(visible: Boolean) {
        keyVisible = visible
        val selection = apiKeyInput.selectionEnd.coerceAtLeast(0)
        apiKeyInput.inputType = if (visible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        apiKeyInput.setSelection(selection.coerceAtMost(apiKeyInput.text.length))
        showKeyButton.setText(if (visible) R.string.hide_key else R.string.show_key)
    }

    private fun confirmClearKey() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_key_title)
            .setMessage(R.string.clear_key_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear) { _, _ ->
                prefs.clearApiKey()
                apiKeyInput.setText("")
                updateKeyHint()
                testResult.visibility = View.GONE
                Toast.makeText(this, R.string.key_cleared, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showInlineMessage(message: String, isError: Boolean) {
        testResult.visibility = View.VISIBLE
        testResult.text = message
        testResult.setTextColor(
            colorAttr(
                if (isError) {
                    androidx.appcompat.R.attr.colorError
                } else {
                    com.google.android.material.R.attr.colorOnSurface
                },
            ),
        )
    }

    private fun hideKeyboard() {
        currentFocus?.let { focus ->
            val imm = getSystemService(android.view.inputmethod.InputMethodManager::class.java)
            imm.hideSoftInputFromWindow(focus.windowToken, 0)
            focus.clearFocus()
        }
    }

    private fun colorAttr(attr: Int): Int =
        MaterialColors.getColor(this, attr, Color.BLACK)

    private fun styleStatus(containerAttr: Int, labelAttr: Int, strokeAttr: Int? = null) {
        statusPill.chipBackgroundColor = ColorStateList.valueOf(colorAttr(containerAttr))
        statusPill.setTextColor(colorAttr(labelAttr))
        if (strokeAttr == null) {
            statusPill.chipStrokeWidth = 0f
        } else {
            statusPill.chipStrokeWidth = resources.displayMetrics.density
            statusPill.chipStrokeColor = ColorStateList.valueOf(colorAttr(strokeAttr))
        }
    }

    private fun applyButtonRole(button: MaterialButton, role: ButtonRole) {
        val primary = colorAttr(androidx.appcompat.R.attr.colorPrimary)
        val onPrimary = colorAttr(com.google.android.material.R.attr.colorOnPrimary)
        val secondaryContainer = colorAttr(com.google.android.material.R.attr.colorSecondaryContainer)
        val onSecondaryContainer = colorAttr(com.google.android.material.R.attr.colorOnSecondaryContainer)
        val outline = colorAttr(com.google.android.material.R.attr.colorOutline)
        val onSurface = colorAttr(com.google.android.material.R.attr.colorOnSurface)
        val disabledContent = MaterialColors.compositeARGBWithAlpha(onSurface, 97)
        val disabledContainer = MaterialColors.compositeARGBWithAlpha(onSurface, 31)

        fun stateful(enabled: Int, disabled: Int) = ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(disabled, enabled),
        )

        button.elevation = 0f
        when (role) {
            ButtonRole.FILLED -> {
                button.backgroundTintList = stateful(primary, disabledContainer)
                button.setTextColor(stateful(onPrimary, disabledContent))
                button.strokeWidth = 0
            }
            ButtonRole.TONAL -> {
                button.backgroundTintList = stateful(secondaryContainer, disabledContainer)
                button.setTextColor(stateful(onSecondaryContainer, disabledContent))
                button.strokeWidth = 0
            }
            ButtonRole.OUTLINED -> {
                button.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                button.setTextColor(stateful(onSurface, disabledContent))
                button.strokeColor = stateful(outline, disabledContent)
                button.strokeWidth =
                    (resources.displayMetrics.density + 0.5f).toInt().coerceAtLeast(1)
            }
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 2001
    }
}
