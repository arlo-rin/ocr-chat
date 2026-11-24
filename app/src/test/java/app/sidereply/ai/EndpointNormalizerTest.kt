package app.sidereply.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointNormalizerTest {

    @Test
    fun deepSeekRootGetsChatCompletionsPath() {
        assertEquals(
            "https://api.deepseek.com/chat/completions",
            EndpointNormalizer.normalize("https://api.deepseek.com"),
        )
        assertEquals(
            "https://api.deepseek.com/chat/completions",
            EndpointNormalizer.normalize("https://api.deepseek.com/"),
        )
    }

    @Test
    fun explicitV1BaseGetsChatCompletionsPath() {
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            EndpointNormalizer.normalize("https://api.deepseek.com/v1"),
        )
    }

    @Test
    fun otherProviderRootGetsV1Path() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            EndpointNormalizer.normalize("https://api.openai.com"),
        )
    }

    @Test
    fun fullEndpointIsPreserved() {
        assertEquals(
            "https://api.deepseek.com/chat/completions",
            EndpointNormalizer.normalize("https://api.deepseek.com/chat/completions"),
        )
        assertEquals(
            "https://example.com/custom/v1/chat/completions",
            EndpointNormalizer.normalize("https://example.com/custom/v1/chat/completions/"),
        )
    }

    @Test
    fun queryStringIsPreserved() {
        assertEquals(
            "https://example.com/v1/chat/completions?api-version=2025-01-01",
            EndpointNormalizer.normalize("https://example.com?api-version=2025-01-01"),
        )
    }

    @Test
    fun localHttpIsAllowedButRemoteHttpIsNot() {
        assertTrue(EndpointNormalizer.isLocal("http://10.0.2.2:8787"))
        assertEquals(
            "http://10.0.2.2:8787/v1/chat/completions",
            EndpointNormalizer.normalize("http://10.0.2.2:8787"),
        )
        assertFalse(EndpointNormalizer.isLocal("http://192.168.1.10:8787"))

        val error = assertThrows(EndpointNormalizer.InvalidEndpointException::class.java) {
            EndpointNormalizer.normalize("http://api.example.com/v1")
        }
        assertEquals(EndpointNormalizer.Error.CLEARTEXT_NOT_ALLOWED, error.error)
    }

    @Test
    fun unsupportedSchemeIsRejected() {
        val error = assertThrows(EndpointNormalizer.InvalidEndpointException::class.java) {
            EndpointNormalizer.normalize("ftp://api.example.com")
        }
        assertEquals(EndpointNormalizer.Error.UNSUPPORTED_SCHEME, error.error)
    }
}
