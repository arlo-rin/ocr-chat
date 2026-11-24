package app.sidereply.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyPromptsTest {

    @Test
    fun systemPromptLocksTheUserPerspective() {
        val prompt = ReplyPrompts.SYSTEM

        assertTrue(prompt.contains("发送者视角"))
        assertTrue(prompt.contains("不要替对方说话"))
        assertTrue(prompt.contains("这是对方口吻，禁止输出"))
        assertTrue(prompt.contains("截图"))
        assertTrue(prompt.contains("彩色气泡"))
    }

    @Test
    fun userPromptRepeatsThePerspectiveRule() {
        val prompt = ReplyPrompts.userPrompt(emptyList())

        assertTrue(prompt.contains("你不是对方"))
        assertTrue(prompt.contains("不要替对方回复"))
        assertTrue(prompt.contains("截图"))
        assertFalse(prompt.contains("上一轮"))
    }

    @Test
    fun regenerationCarriesPreviousSuggestionsAndAsksForNewOnes() {
        val previous = listOf(
            ReplySuggestion("自然", "周六晚上可以。"),
            ReplySuggestion("稳妥", "我确认下时间再回你。"),
        )

        val prompt = ReplyPrompts.userPrompt(previous)

        assertTrue(prompt.contains("周六晚上可以。"))
        assertTrue(prompt.contains("我确认下时间再回你。"))
        assertTrue(prompt.contains("换 3 条新的表达"))
        assertTrue(prompt.contains("保持“我”发给对方的视角"))
    }

    @Test
    fun customStyleIsAppliedToEveryReplyButCannotChangePerspective() {
        val prompt = ReplyPrompts.userPrompt(previous = emptyList(), style = "严谨")

        assertTrue(prompt.contains("【本轮指定风格】严谨"))
        assertTrue(prompt.contains("三条建议都必须明显符合这个风格"))
        assertTrue(prompt.contains("不能改变“我”发给对方的身份"))
        assertTrue(prompt.contains("本轮三条回复都必须符合「严谨」风格"))
    }

    @Test
    fun blankStyleDoesNotAddACustomStyleSection() {
        val prompt = ReplyPrompts.userPrompt(emptyList(), "   ")

        assertFalse(prompt.contains("【本轮指定风格】"))
    }
}
