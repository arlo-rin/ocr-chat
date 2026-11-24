package app.sidereply.ui.main

import app.sidereply.data.AppPreferences

enum class MainTab {
    HOME,
    MODEL,
    SETTINGS,
}

data class MainUiState(
    val selectedTab: MainTab = MainTab.HOME,
    val serviceRunning: Boolean = false,
    val rootStarting: Boolean = false,
    val overlayGranted: Boolean = false,
    val usageGranted: Boolean = false,
    val endpoint: String = "",
    val model: String = "",
    val apiKeyInput: String = "",
    val keyVisible: Boolean = false,
    val hasSavedKey: Boolean = false,
    val savedKeyTail: String = "",
    val endpointError: String? = null,
    val modelError: String? = null,
    val testing: Boolean = false,
    val inlineMessage: String? = null,
    val inlineMessageIsError: Boolean = false,
    val wechatOnly: Boolean = true,
    val dualAppSupport: Boolean = true,
    val themeMode: AppPreferences.ThemeMode = AppPreferences.ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val showClearKeyDialog: Boolean = false,
    val rootErrorMessage: String? = null,
    val showRootCommandsSheet: Boolean = false,
) {
    val savedEndpointHost: String
        get() = runCatching { java.net.URI(endpoint.trim()).host.orEmpty() }
            .getOrDefault("")
            .ifBlank { endpoint.trim() }

    val isAiReady: Boolean
        get() = endpoint.isNotBlank() &&
            model.isNotBlank() &&
            (hasSavedKey || apiKeyInput.isNotBlank() || app.sidereply.ai.EndpointNormalizer.isLocal(endpoint))
}
