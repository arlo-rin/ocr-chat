package app.sidereply.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.UnknownServiceException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Plain HttpURLConnection client so the app has no networking framework dependency.
 * Any endpoint implementing the OpenAI chat/completions contract can be used.
 */
class AiClient {

    /**
     * Sends the chat screenshot as an OpenAI `image_url` part; the model reads the bubbles
     * itself. The app no longer runs any local OCR.
     */
    fun suggestReplies(
        config: AiConfig,
        screenshotJpeg: ByteArray,
        previousSuggestions: List<ReplySuggestion> = emptyList(),
        style: String = "",
    ): List<ReplySuggestion> {
        if (screenshotJpeg.isEmpty()) throw AiException(AiErrorKind.EMPTY_RESPONSE)
        val content = requestCompletion(
            config = config,
            messages = JSONArray()
                .put(message("system", ReplyPrompts.SYSTEM))
                .put(
                    JSONObject().put("role", "user").put(
                        "content",
                        userContent(screenshotJpeg, previousSuggestions, style),
                    ),
                ),
            maxTokens = 700,
            temperature = 0.65,
        )
        val suggestions = ReplyParser.parse(content)
        if (suggestions.isEmpty()) throw AiException(AiErrorKind.EMPTY_RESPONSE)
        return suggestions
    }

    /** The OpenAI-compatible `user` content: a text instruction plus the screenshot. */
    internal fun userContent(
        screenshotJpeg: ByteArray,
        previous: List<ReplySuggestion>,
        style: String,
    ): JSONArray = JSONArray()
        .put(
            JSONObject()
                .put("type", "text")
                .put("text", ReplyPrompts.userPrompt(previous, style)),
        )
        .put(
            JSONObject()
                .put("type", "image_url")
                .put("image_url", JSONObject().put("url", dataUrl(screenshotJpeg))),
        )

    internal fun dataUrl(jpeg: ByteArray): String =
        "data:image/jpeg;base64," + java.util.Base64.getEncoder().encodeToString(jpeg)

    /**
     * Makes a real, small chat completion request. Returns the assistant's text so the
     * settings screen can show that the endpoint, model and key all work together.
     */
    fun testConnection(config: AiConfig): String {
        return requestCompletion(
            config = config,
            messages = JSONArray()
                .put(message("system", "You are a connectivity test. Follow the user exactly."))
                .put(message("user", "这是一次连接测试。请只回复两个字：正常")),
            maxTokens = 16,
            temperature = 0.0,
        ).trim()
    }

    private fun requestCompletion(
        config: AiConfig,
        messages: JSONArray,
        maxTokens: Int,
        temperature: Double,
    ): String {
        val endpoint = parseEndpoint(config.endpoint)
        if (config.model.isBlank()) throw AiException(AiErrorKind.INVALID_REQUEST)

        val payload = JSONObject()
            .put("model", config.model)
            .put("messages", messages)
            .put("temperature", temperature)
            .put("max_tokens", maxTokens)
            .put("stream", false)
            .apply {
                // DeepSeek enables thinking by default. Chat replies should be fast and
                // cheap, and a small max_tokens budget must be left for visible content.
                if (endpoint.host.equals("api.deepseek.com", ignoreCase = true)) {
                    put("thinking", JSONObject().put("type", "disabled"))
                }
            }
            .toString()
        val payloadBytes = payload.toByteArray(StandardCharsets.UTF_8)

        val connection = try {
            endpoint.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw AiException(AiErrorKind.NETWORK, e)
        }

        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = TimeUnit.SECONDS.toMillis(15).toInt()
            connection.readTimeout = TimeUnit.SECONDS.toMillis(50).toInt()
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "SideReply-Android/0.2.5")
            if (config.apiKey.isNotBlank()) {
                connection.setRequestProperty("Authorization", "Bearer ${config.apiKey.trim()}")
            }
            connection.setFixedLengthStreamingMode(payloadBytes.size)
            connection.outputStream.use { it.write(payloadBytes) }

            val status = connection.responseCode
            val body = readResponseBody(connection, status)
            if (status !in 200..299) {
                throw httpError(status, body)
            }
            return extractContent(body)
        } catch (e: AiException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw AiException(AiErrorKind.NETWORK, e)
        } catch (e: UnknownServiceException) {
            throw AiException(AiErrorKind.CLEARTEXT_NOT_ALLOWED, e)
        } catch (e: IOException) {
            val message = e.message.orEmpty()
            if (message.contains("CLEARTEXT", ignoreCase = true)) {
                throw AiException(AiErrorKind.CLEARTEXT_NOT_ALLOWED, e)
            }
            throw AiException(AiErrorKind.NETWORK, e)
        } catch (e: Exception) {
            throw AiException(AiErrorKind.UNKNOWN, e)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseEndpoint(raw: String): URL {
        val normalized = try {
            EndpointNormalizer.normalize(raw)
        } catch (e: EndpointNormalizer.InvalidEndpointException) {
            val kind = when (e.error) {
                EndpointNormalizer.Error.CLEARTEXT_NOT_ALLOWED ->
                    AiErrorKind.CLEARTEXT_NOT_ALLOWED
                else -> AiErrorKind.BAD_ENDPOINT
            }
            throw AiException(kind, e)
        }
        return try {
            URI(normalized).toURL()
        } catch (e: Exception) {
            throw AiException(AiErrorKind.BAD_ENDPOINT, e)
        }
    }

    private fun readResponseBody(connection: HttpURLConnection, status: Int): String {
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

    private fun extractContent(body: String): String {
        val root = try {
            JSONObject(body)
        } catch (e: Exception) {
            throw AiException(AiErrorKind.UNKNOWN, e, "AI 服务返回了无法解析的内容")
        }

        if (root.has("error")) {
            throw AiException(AiErrorKind.UNKNOWN, detail = providerErrorMessage(root.opt("error")))
        }

        val choices = root.optJSONArray("choices")
            ?: throw AiException(AiErrorKind.EMPTY_RESPONSE, detail = "响应中没有 choices")
        val first = choices.optJSONObject(0)
            ?: throw AiException(AiErrorKind.EMPTY_RESPONSE)
        val message = first.optJSONObject("message")
            ?: throw AiException(AiErrorKind.EMPTY_RESPONSE)

        val contentValue = message.opt("content")
        val content = when (contentValue) {
            is String -> contentValue
            is JSONArray -> buildString {
                for (index in 0 until contentValue.length()) {
                    val part = contentValue.optJSONObject(index) ?: continue
                    if (part.optString("type") == "text" || part.has("text")) {
                        append(part.optString("text"))
                    }
                }
            }

            else -> ""
        }
        if (content.isBlank()) {
            throw AiException(AiErrorKind.EMPTY_RESPONSE)
        }
        return content
    }

    private fun httpError(status: Int, body: String): AiException {
        val detail = runCatching {
            val obj = JSONObject(body)
            providerErrorMessage(obj.opt("error")) ?: obj.optString("message")
        }.getOrNull()?.take(280)

        val kind = when (status) {
            400, 422 -> AiErrorKind.INVALID_REQUEST
            401, 403 -> AiErrorKind.AUTH
            404 -> AiErrorKind.NOT_FOUND
            408, 409, 429 -> AiErrorKind.RATE_LIMIT
            in 500..599 -> AiErrorKind.SERVER
            else -> AiErrorKind.UNKNOWN
        }
        return AiException(kind, detail = detail?.takeIf { it.isNotBlank() })
    }

    private fun providerErrorMessage(error: Any?): String? {
        return when (error) {
            is JSONObject -> error.optString("message").takeIf { it.isNotBlank() }
            is String -> error.takeIf { it.isNotBlank() }
            else -> null
        }
    }

    private fun message(role: String, content: String): JSONObject =
        JSONObject().put("role", role).put("content", content)
}
