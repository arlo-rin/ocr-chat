package app.sidereply.data

import android.content.Context
import app.sidereply.ai.AiConfig

/**
 * User-visible settings. Only the encrypted API key is stored; screenshots are never written
 * to disk — they only pass through memory on their way to the AI endpoint.
 */
class AppPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var endpoint: String
        get() = prefs.getString(KEY_ENDPOINT, DEFAULT_ENDPOINT).orEmpty().trim()
        set(value) = prefs.edit().putString(KEY_ENDPOINT, value.trim()).apply()

    var model: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty().trim()
        set(value) = prefs.edit().putString(KEY_MODEL, value.trim()).apply()

    /** When true, the bubble is also shown outside WeChat (useful for testing). */
    var showInAllApps: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ALL_APPS, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_ALL_APPS, value).apply()

    /**
     * When true, the service also probes the globally focused window through root, so
     * 「应用双开 / 应用分身」 copies (which run outside this app's Android user) are seen.
     * Probing only happens on devices that actually have a second Android user.
     */
    var dualAppSupport: Boolean
        get() = prefs.getBoolean(KEY_DUAL_APP_SUPPORT, true)
        set(value) = prefs.edit().putBoolean(KEY_DUAL_APP_SUPPORT, value).apply()

    enum class ThemeMode(val storageValue: Int) {
        SYSTEM(0),
        LIGHT(1),
        DARK(2),
        ;

        companion object {
            fun fromStorage(value: Int): ThemeMode =
                entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }

    var bubbleY: Int
        get() = prefs.getInt(KEY_BUBBLE_Y, -1)
        set(value) = prefs.edit().putInt(KEY_BUBBLE_Y, value).apply()

    var themeMode: ThemeMode
        get() = ThemeMode.fromStorage(prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.storageValue))
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value.storageValue).apply()

    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()

    fun apiKey(): String {
        val encrypted = prefs.getString(KEY_API_KEY_CIPHER, null) ?: return ""
        return SecretStore.decrypt(encrypted).orEmpty()
    }

    fun hasStoredApiKey(): Boolean = apiKey().isNotBlank()

    fun saveApiKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return
        val encrypted = SecretStore.encrypt(trimmed)
        prefs.edit().putString(KEY_API_KEY_CIPHER, encrypted).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY_CIPHER).apply()
    }

    fun toAiConfig(apiKeyOverride: String? = null): AiConfig = AiConfig(
        endpoint = endpoint,
        model = model,
        apiKey = apiKeyOverride ?: apiKey(),
    )

    companion object {
        const val WECHAT_PACKAGE = "com.tencent.mm"
        const val DEFAULT_ENDPOINT = "https://api.deepseek.com/chat/completions"
        const val DEFAULT_MODEL = "deepseek-flash"

        private const val PREFS_NAME = "side_reply_prefs"
        private const val KEY_ENDPOINT = "endpoint"
        private const val KEY_MODEL = "model"
        private const val KEY_API_KEY_CIPHER = "api_key_cipher"
        private const val KEY_SHOW_ALL_APPS = "show_in_all_apps"
        private const val KEY_DUAL_APP_SUPPORT = "dual_app_support"
        private const val KEY_BUBBLE_Y = "bubble_y"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    }
}
