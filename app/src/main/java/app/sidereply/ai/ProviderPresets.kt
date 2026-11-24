package app.sidereply.ai

/**
 * Vision-capable OpenAI-compatible provider presets for quick setup in the Model tab.
 */
data class VisionProviderPreset(
    val id: String,
    val label: String,
    val endpoint: String,
    val model: String,
)

object ProviderPresets {
    val ALL: List<VisionProviderPreset> = listOf(
        VisionProviderPreset(
            id = "openai",
            label = "OpenAI",
            endpoint = "https://api.openai.com/v1/chat/completions",
            model = "gpt-4o-mini",
        ),
        VisionProviderPreset(
            id = "qwen",
            label = "阿里云通义",
            endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            model = "qwen-vl-plus",
        ),
        VisionProviderPreset(
            id = "glm",
            label = "智谱 GLM",
            endpoint = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            model = "glm-4v-flash",
        ),
        VisionProviderPreset(
            id = "kimi",
            label = "Kimi",
            endpoint = "https://api.moonshot.cn/v1/chat/completions",
            model = "moonshot-v1-8k-vision-preview",
        ),
        VisionProviderPreset(
            id = "mock",
            label = "本地调试",
            endpoint = "http://10.0.2.2:8787/v1/chat/completions",
            model = "mock-chat",
        ),
    )

    fun matchingId(endpoint: String): String? {
        val clean = endpoint.trim().lowercase()
        if (clean.isEmpty()) return null
        return ALL.firstOrNull { preset ->
            val host = runCatching { java.net.URI(preset.endpoint).host?.lowercase() }.getOrNull()
            host != null && clean.contains(host)
        }?.id
    }
}
