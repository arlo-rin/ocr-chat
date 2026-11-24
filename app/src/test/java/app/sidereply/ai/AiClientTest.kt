package app.sidereply.ai

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class AiClientTest {

    private val client = AiClient()

    @Test
    fun userContentCarriesTextAndImageParts() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x00, 0x11, 0x22)

        val content = client.userContent(jpeg, emptyList(), "")

        assertEquals(2, content.length())
        val textPart = content.getJSONObject(0)
        assertEquals("text", textPart.getString("type"))
        assertTrue(textPart.getString("text").contains("截图"))

        val imagePart = content.getJSONObject(1)
        assertEquals("image_url", imagePart.getString("type"))
        val url = imagePart.getJSONObject("image_url").getString("url")
        assertTrue(url.startsWith("data:image/jpeg;base64,"))
        assertArrayEquals(jpeg, Base64.getDecoder().decode(url.substringAfter(',')))
    }

    @Test
    fun styleAndPreviousSuggestionsReachTheTextPart() {
        val previous = listOf(ReplySuggestion("自然", "好的没问题"))

        val content = client.userContent(byteArrayOf(1, 2, 3), previous, "严谨")
        val text = content.getJSONObject(0).getString("text")

        assertTrue(text.contains("【本轮指定风格】严谨"))
        assertTrue(text.contains("好的没问题"))
    }

    @Test
    fun dataUrlIsStandardBase64Jpeg() {
        val jpeg = byteArrayOf(1, 2, 3, 4, 5)

        val url = client.dataUrl(jpeg)

        assertTrue(url.startsWith("data:image/jpeg;base64,"))
        assertArrayEquals(jpeg, Base64.getDecoder().decode(url.substringAfter(',')))
    }
}
