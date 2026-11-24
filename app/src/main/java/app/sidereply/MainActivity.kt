package app.sidereply

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import app.sidereply.ai.AiClient
import app.sidereply.ai.AiConfig
import app.sidereply.ai.AiErrorMessages
import app.sidereply.ai.EndpointNormalizer
import app.sidereply.ai.VisionProviderPreset
import app.sidereply.data.AppPreferences
import app.sidereply.root.RootManager
import app.sidereply.service.FloatingAssistantService
import app.sidereply.ui.main.MainTab
import app.sidereply.ui.main.MainUiState
import app.sidereply.ui.main.SideReplyApp
import app.sidereply.ui.theme.SideReplyTheme
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences
    private val worker = Executors.newSingleThreadExecutor()
    private val snackbarHostState = SnackbarHostState()
    private var uiState by mutableStateOf(MainUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)
        loadInitialState()

        setContent {
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (uiState.themeMode) {
                AppPreferences.ThemeMode.SYSTEM -> systemDark
                AppPreferences.ThemeMode.LIGHT -> false
                AppPreferences.ThemeMode.DARK -> true
            }

            LaunchedEffect(darkTheme) {
                val barStyle = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(
                    statusBarStyle = barStyle,
                    navigationBarStyle = barStyle,
                )
            }

            val host = remember { snackbarHostState }
            SideReplyTheme(
                themeMode = uiState.themeMode,
                dynamicColor = uiState.dynamicColor,
            ) {
                SideReplyApp(
                    state = uiState,
                    snackbarHostState = host,
                    onSelectTab = { tab ->
                        uiState = uiState.copy(selectedTab = tab)
                    },
                    onToggleService = {
                        if (FloatingAssistantService.isRunning) {
                            stopRootOverlay()
                        } else {
                            startRootOverlay()
                        }
                    },
                    onCopySampleReply = { text ->
                        val clipboard = getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("侧答示例回复", text))
                        showSnackbar(getString(R.string.home_preview_copied))
                    },
                    onEndpointChange = { value ->
                        uiState = uiState.copy(endpoint = value, endpointError = null)
                    },
                    onModelChange = { value ->
                        uiState = uiState.copy(model = value, modelError = null)
                    },
                    onApiKeyChange = { value ->
                        uiState = uiState.copy(apiKeyInput = value)
                    },
                    onApplyPreset = { preset -> applyPreset(preset) },
                    onToggleKeyVisibility = { toggleKeyVisibility() },
                    onRequestClearKey = {
                        uiState = uiState.copy(showClearKeyDialog = true)
                    },
                    onConfirmClearKey = { clearSavedKey() },
                    onDismissClearKey = {
                        uiState = uiState.copy(showClearKeyDialog = false)
                    },
                    onSaveSettings = { saveSettings() },
                    onTestConnection = { testConnection() },
                    onWechatOnlyChange = { checked ->
                        prefs.showInAllApps = !checked
                        uiState = uiState.copy(wechatOnly = checked)
                    },
                    onDualAppChange = { checked ->
                        prefs.dualAppSupport = checked
                        uiState = uiState.copy(dualAppSupport = checked)
                    },
                    onThemeModeChange = { mode ->
                        prefs.themeMode = mode
                        uiState = uiState.copy(themeMode = mode)
                        notifyServiceThemeChanged()
                    },
                    onDynamicColorChange = { enabled ->
                        prefs.dynamicColor = enabled
                        uiState = uiState.copy(dynamicColor = enabled)
                        notifyServiceThemeChanged()
                    },
                    onOpenRootCommands = {
                        uiState = uiState.copy(showRootCommandsSheet = true)
                    },
                    onDismissRootCommands = {
                        uiState = uiState.copy(showRootCommandsSheet = false)
                    },
                    onDismissRootError = {
                        uiState = uiState.copy(rootErrorMessage = null)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRootServiceStatus()
        refreshSavedKeyState()
    }

    override fun onPause() {
        if (uiState.keyVisible) {
            uiState = uiState.copy(keyVisible = false)
        }
        super.onPause()
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun loadInitialState() {
        val storedKey = readSavedKey()
        val hasKey = storedKey.isNotBlank()
        uiState = MainUiState(
            selectedTab = MainTab.HOME,
            serviceRunning = FloatingAssistantService.isRunning,
            overlayGranted = RootManager.canDrawOverlays(this),
            usageGranted = RootManager.hasUsageAccess(this),
            endpoint = prefs.endpoint,
            model = prefs.model,
            apiKeyInput = "",
            keyVisible = false,
            hasSavedKey = hasKey,
            savedKeyTail = if (hasKey) keyTail(storedKey) else "",
            wechatOnly = !prefs.showInAllApps,
            dualAppSupport = prefs.dualAppSupport,
            themeMode = prefs.themeMode,
            dynamicColor = prefs.dynamicColor,
        )
    }

    private fun refreshRootServiceStatus() {
        uiState = uiState.copy(
            serviceRunning = FloatingAssistantService.isRunning,
            overlayGranted = RootManager.canDrawOverlays(this),
            usageGranted = RootManager.hasUsageAccess(this),
        )
    }

    private fun refreshSavedKeyState() {
        val storedKey = readSavedKey()
        val hasKey = storedKey.isNotBlank()
        uiState = uiState.copy(
            hasSavedKey = hasKey,
            savedKeyTail = if (hasKey) keyTail(storedKey) else "",
        )
    }

    private fun startRootOverlay() {
        if (uiState.rootStarting) return
        uiState = uiState.copy(rootStarting = true)

        val appContext = applicationContext
        worker.execute {
            val result = if (RootManager.isReady(appContext)) {
                Result.success(Unit)
            } else {
                runCatching { RootManager.grantPermissionsViaRoot(appContext) }
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                uiState = uiState.copy(rootStarting = false)
                result.fold(
                    onSuccess = {
                        try {
                            startForegroundService(
                                Intent(this, FloatingAssistantService::class.java),
                            )
                            requestNotificationPermissionIfNeeded()
                            showSnackbar(getString(R.string.toast_overlay_started))
                            window.decorView.postDelayed({
                                if (!isFinishing && !isDestroyed) refreshRootServiceStatus()
                            }, 450)
                        } catch (e: Exception) {
                            refreshRootServiceStatus()
                            showSnackbar(
                                getString(
                                    R.string.root_error_start,
                                    e.message ?: getString(R.string.root_error_command),
                                ),
                            )
                        }
                    },
                    onFailure = { error ->
                        refreshRootServiceStatus()
                        uiState = uiState.copy(rootErrorMessage = rootSetupErrorMessage(error))
                    },
                )
            }
        }
    }

    private fun stopRootOverlay() {
        try {
            stopService(Intent(this, FloatingAssistantService::class.java))
            showSnackbar(getString(R.string.toast_overlay_stopped))
            window.decorView.postDelayed({
                if (!isFinishing && !isDestroyed) refreshRootServiceStatus()
            }, 300)
        } catch (e: Exception) {
            showSnackbar(
                getString(
                    R.string.root_error_start,
                    e.message ?: getString(R.string.root_error_command),
                ),
            )
        }
    }

    private fun notifyServiceThemeChanged() {
        if (!FloatingAssistantService.isRunning) return
        runCatching {
            startService(
                Intent(this, FloatingAssistantService::class.java).apply {
                    action = FloatingAssistantService.ACTION_REFRESH_THEME
                },
            )
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

    private fun applyPreset(preset: VisionProviderPreset) {
        uiState = uiState.copy(
            endpoint = preset.endpoint,
            model = preset.model,
            endpointError = null,
            modelError = null,
            inlineMessage = getString(R.string.preset_applied, preset.label),
            inlineMessageIsError = false,
        )
    }

    private fun toggleKeyVisibility() {
        if (uiState.keyVisible) {
            uiState = uiState.copy(keyVisible = false)
            return
        }
        if (uiState.apiKeyInput.isNotEmpty()) {
            uiState = uiState.copy(keyVisible = true)
            return
        }
        val storedKey = readSavedKey()
        if (storedKey.isEmpty()) {
            showSnackbar(getString(R.string.key_nothing_to_show))
            return
        }
        uiState = uiState.copy(
            apiKeyInput = storedKey,
            keyVisible = true,
        )
    }

    private fun clearSavedKey() {
        prefs.clearApiKey()
        uiState = uiState.copy(
            apiKeyInput = "",
            keyVisible = false,
            hasSavedKey = false,
            savedKeyTail = "",
            showClearKeyDialog = false,
            inlineMessage = null,
        )
        showSnackbar(getString(R.string.key_cleared))
    }

    private fun saveSettings() {
        val form = validateForm() ?: return

        val typedKey = uiState.apiKeyInput.trim()
        var savedNewKey = false
        if (typedKey.isNotEmpty()) {
            try {
                prefs.saveApiKey(typedKey)
            } catch (_: Exception) {
                uiState = uiState.copy(
                    inlineMessage = getString(R.string.key_save_failed),
                    inlineMessageIsError = true,
                )
                return
            }
            savedNewKey = true
        }

        prefs.endpoint = form.first
        prefs.model = form.second

        val storedKey = readSavedKey()
        val hasKey = storedKey.isNotBlank()
        val message = if (hasKey || EndpointNormalizer.isLocal(form.first)) {
            getString(R.string.saved)
        } else {
            getString(R.string.saved_without_key)
        }
        uiState = uiState.copy(
            endpoint = form.first,
            model = form.second,
            apiKeyInput = if (savedNewKey) typedKey else uiState.apiKeyInput,
            keyVisible = false,
            hasSavedKey = hasKey,
            savedKeyTail = if (hasKey) keyTail(storedKey) else "",
            inlineMessage = message,
            inlineMessageIsError = false,
        )
        showSnackbar(
            if (savedNewKey) getString(R.string.key_saved_toast) else message,
        )
    }

    private fun testConnection() {
        if (uiState.testing) return
        val form = validateForm() ?: return

        val key = uiState.apiKeyInput.trim().ifBlank { readSavedKey() }
        val config = AiConfig(endpoint = form.first, model = form.second, apiKey = key)

        uiState = uiState.copy(
            testing = true,
            inlineMessage = getString(R.string.test_testing),
            inlineMessageIsError = false,
        )

        worker.execute {
            val result = runCatching { AiClient().testConnection(config) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
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
                        uiState = uiState.copy(
                            testing = false,
                            inlineMessage = message,
                            inlineMessageIsError = false,
                        )
                    },
                    onFailure = { error ->
                        uiState = uiState.copy(
                            testing = false,
                            inlineMessage = AiErrorMessages.forThrowable(this, error),
                            inlineMessageIsError = true,
                        )
                    },
                )
            }
        }
    }

    private fun validateForm(): Pair<String, String>? {
        val rawEndpoint = uiState.endpoint.trim()
        val model = uiState.model.trim()

        if (rawEndpoint.isEmpty()) {
            val msg = getString(R.string.endpoint_required)
            uiState = uiState.copy(endpointError = msg, inlineMessage = msg, inlineMessageIsError = true)
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
            uiState = uiState.copy(endpointError = message, inlineMessage = message, inlineMessageIsError = true)
            return null
        }

        if (model.isEmpty()) {
            val msg = getString(R.string.model_required)
            uiState = uiState.copy(
                endpoint = endpoint,
                endpointError = null,
                modelError = msg,
                inlineMessage = msg,
                inlineMessageIsError = true,
            )
            return null
        }

        uiState = uiState.copy(
            endpoint = endpoint,
            model = model,
            endpointError = null,
            modelError = null,
        )
        return endpoint to model
    }

    private fun readSavedKey(): String =
        runCatching { prefs.apiKey() }.getOrNull().orEmpty()

    private fun keyTail(key: String): String =
        if (key.length >= 8) key.takeLast(4) else "••••"

    private fun showSnackbar(message: String) {
        lifecycleScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 2001
    }
}
