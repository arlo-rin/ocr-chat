package app.sidereply.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyParserTest {

    @Test
    fun parsesExpectedObject() {
        val raw = """
            {"replies":[
              {"style":"自然","text":"好呀，周六晚上怎么样？"},
              {"style":"稳妥","text":"可以呀，你定时间，我来订位。"},
              {"style":"轻松","text":"有空，终于想起我啦。"}
            ]}
        """.trimIndent()

        val result = ReplyParser.parse(raw)

        assertEquals(3, result.size)
        assertEquals("自然", result[0].style)
        assertEquals("好呀，周六晚上怎么样？", result[0].text)
        assertEquals("轻松", result[2].style)
    }

    @Test
    fun parsesFencedJsonAndSurroundingText() {
        val raw = """
            好的，下面是建议：
            ```json
            {"replies":[{"style":"自然","text":"可以的，几点？"}]}
            ```
        """.trimIndent()

        val result = ReplyParser.parse(raw)

        assertEquals(1, result.size)
        assertEquals("可以的，几点？", result.first().text)
    }

    @Test
    fun parsesBareArray() {
        val raw = """
            ["收到，我晚点回复你。", "好，没问题。"]
        """.trimIndent()

        val result = ReplyParser.parse(raw)

        assertEquals(2, result.size)
        assertTrue(result[0].style.isNotBlank())
    }

    @Test
    fun fallsBackToNumberedPlainLines() {
        val raw = """
            自然：好呀，周六晚上怎么样？
            2. 稳妥：可以，你定时间就好。
            - 轻松：当然有空。
        """.trimIndent()

        val result = ReplyParser.parse(raw)

        assertEquals(3, result.size)
        assertEquals("自然", result[0].style)
        assertEquals("稳妥", result[1].style)
        assertEquals("轻松", result[2].style)
        assertTrue(result.none { it.text.startsWith("2.") })
    }

    @Test
    fun deduplicatesAndCapsAtThree() {
        val raw = """
            {"replies":[
              {"style":"自然","text":"好的"},
              {"style":"稳妥","text":"好的"},
              {"style":"轻松","text":"没问题"},
              {"style":"关心","text":"早点休息"},
              {"style":"其他","text":"第四条"}
            ]}
        """.trimIndent()

        val result = ReplyParser.parse(raw)

        assertEquals(3, result.size)
        assertEquals("好的", result[0].text)
        assertEquals("没问题", result[1].text)
        assertEquals("早点休息", result[2].text)
    }

    @Test
    fun normalizesWhitespaceAndQuotes() {
        val raw = """{"replies":[{"style":"自然","text":"  “好呀，\n周六见。”  "}]}"""

        val result = ReplyParser.parse(raw)

        assertEquals("好呀， 周六见。", result.single().text)
    }

    @Test
    fun returnsEmptyForEmptyInput() {
        assertTrue(ReplyParser.parse("   ").isEmpty())
    }
}
