package app.sidereply.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Models are asked for a small JSON object, but in practice they sometimes return a
 * fenced array, an object with a different key, or plain numbered lines. The parser is
 * deliberately forgiving so a cosmetic formatting difference never loses a reply.
 */
object ReplyParser {

    private val stylePrefix = Regex(
        "^(自然|稳妥|轻松|礼貌|幽默|简洁|关心|直接|温柔|正式|随意)\\s*[:：]\\s*",
    )
    private val numberedPrefix = Regex("^(?:[-*•]\\s*|\\d{1,2}\\s*[.、)．]\\s*)+")
    private val surroundingQuotes = Regex("^[\"'“”‘’「」]+|[\"'“”‘’「」]+$")
    private val whitespace = Regex("\\s+")

    fun parse(raw: String): List<ReplySuggestion> {
        val cleaned = stripCodeFence(raw.trim())
        if (cleaned.isEmpty()) return emptyList()

        parseJson(cleaned)?.takeIf { it.isNotEmpty() }?.let { return it.take(3) }
        return parsePlainText(cleaned).take(3)
    }

    private fun parseJson(raw: String): List<ReplySuggestion>? {
        val candidates = buildList {
            add(raw)
            extractBetween(raw, '{', '}')?.let(::add)
            extractBetween(raw, '[', ']')?.let(::add)
        }.distinct()

        for (candidate in candidates) {
            val array = when {
                candidate.startsWith("{") -> {
                    val obj = runCatching { JSONObject(candidate) }.getOrNull()
                        ?: continue
                    obj.optJSONArray("replies")
                        ?: obj.optJSONArray("suggestions")
                        ?: obj.optJSONArray("data")
                }

                candidate.startsWith("[") -> runCatching { JSONArray(candidate) }.getOrNull()
                else -> null
            }
            if (array != null) {
                val parsed = parseArray(array)
                if (parsed.isNotEmpty()) return parsed
            }
        }
        return null
    }

    private fun parseArray(array: JSONArray): List<ReplySuggestion> {
        val result = mutableListOf<ReplySuggestion>()
        for (index in 0 until array.length()) {
            val item = array.opt(index)
            val style: String
            val text: String
            when (item) {
                is JSONObject -> {
                    style = firstNonBlank(
                        item.optString("style"),
                        item.optString("tone"),
                        item.optString("label"),
                    ) ?: "建议 ${index + 1}"
                    text = firstNonBlank(
                        item.optString("text"),
                        item.optString("reply"),
                        item.optString("content"),
                        item.optString("message"),
                    ).orEmpty()
                }

                is String -> {
                    style = "建议 ${index + 1}"
                    text = item
                }

                else -> continue
            }
            addCleaned(result, style, text)
        }
        return result
    }

    private fun parsePlainText(raw: String): List<ReplySuggestion> {
        val result = mutableListOf<ReplySuggestion>()
        val lines = raw
            .replace(Regex("```(?:json)?"), "")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it == "{" || it == "}" || it == "[" || it == "]" }
            .filterNot { it.startsWith("\"replies\"") }

        for (line in lines) {
            var text = line.replace(numberedPrefix, "")
            val explicitStyle = stylePrefix.find(text)?.groupValues?.getOrNull(1)
            text = text.replace(stylePrefix, "")
            val style = explicitStyle ?: "建议 ${result.size + 1}"
            addCleaned(result, style, text)
        }
        return result
    }

    private fun addCleaned(
        result: MutableList<ReplySuggestion>,
        rawStyle: String,
        rawText: String,
    ) {
        val text = rawText
            .replace("\\n", " ")
            .replace(whitespace, " ")
            .trim()
            .replace(surroundingQuotes, "")
            .trim()
            .take(300)
        if (text.isEmpty()) return
        if (text.equals("replies", ignoreCase = true)) return
        if (result.any { it.text == text }) return
        val style = rawStyle
            .replace(whitespace, " ")
            .trim()
            .ifEmpty { "建议 ${result.size + 1}" }
            .take(12)
        result += ReplySuggestion(style = style, text = text)
    }

    private fun stripCodeFence(text: String): String = text
        .replace(Regex("^```(?:json|JSON)?\\s*"), "")
        .replace(Regex("\\s*```$"), "")
        .trim()

    private fun extractBetween(text: String, start: Char, end: Char): String? {
        val first = text.indexOf(start)
        val last = text.lastIndexOf(end)
        return if (first >= 0 && last > first) text.substring(first, last + 1) else null
    }

    private fun firstNonBlank(vararg values: String): String? =
        values.firstOrNull { it.isNotBlank() }?.trim()
}
