package app.sidereply.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderPresetsTest {

    @Test
    fun allPresetsNormalizeToThemselves() {
        ProviderPresets.ALL.forEach { preset ->
            assertEquals(preset.endpoint, EndpointNormalizer.normalize(preset.endpoint))
        }
    }

    @Test
    fun matchesKnownProviderHosts() {
        assertEquals("openai", ProviderPresets.matchingId("https://api.openai.com/v1/chat/completions"))
        assertEquals("qwen", ProviderPresets.matchingId("https://dashscope.aliyuncs.com/compatible-mode/v1"))
        assertEquals("glm", ProviderPresets.matchingId("https://open.bigmodel.cn/api/paas/v4/chat/completions"))
        assertEquals("kimi", ProviderPresets.matchingId("https://api.moonshot.cn/v1/chat/completions"))
        assertEquals("mock", ProviderPresets.matchingId("http://10.0.2.2:8787/v1/chat/completions"))
        assertNull(ProviderPresets.matchingId("https://custom.example.com/v1/chat/completions"))
        assertNull(ProviderPresets.matchingId(""))
    }
}
