package app.sidereply.ai

import java.net.URI

/**
 * Users reasonably paste the provider's BASE URL (`https://api.deepseek.com`) instead
 * of a full `/chat/completions` endpoint. Normalise both forms so a missing path never
 * turns into a confusing 404.
 */
object EndpointNormalizer {

    enum class Error {
        MALFORMED,
        UNSUPPORTED_SCHEME,
        CLEARTEXT_NOT_ALLOWED,
    }

    class InvalidEndpointException(val error: Error) : Exception(error.name)

    private val LOCAL_HTTP_HOSTS = setOf("10.0.2.2", "localhost", "127.0.0.1")

    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) throw InvalidEndpointException(Error.MALFORMED)

        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            throw InvalidEndpointException(Error.MALFORMED)
        }

        val scheme = uri.scheme?.lowercase()
            ?: throw InvalidEndpointException(Error.MALFORMED)
        if (scheme != "https" && scheme != "http") {
            throw InvalidEndpointException(Error.UNSUPPORTED_SCHEME)
        }

        val host = uri.host?.takeIf { it.isNotBlank() }
            ?: throw InvalidEndpointException(Error.MALFORMED)
        if (scheme == "http" && host.lowercase() !in LOCAL_HTTP_HOSTS) {
            throw InvalidEndpointException(Error.CLEARTEXT_NOT_ALLOWED)
        }

        val rawPath = uri.rawPath.orEmpty()
        val pathWithoutTrailingSlash = rawPath.trimEnd('/')
        val path = when {
            pathWithoutTrailingSlash.endsWith("/chat/completions", ignoreCase = true) ->
                pathWithoutTrailingSlash

            rawPath.isEmpty() || rawPath == "/" -> {
                // DeepSeek documents https://api.deepseek.com as the OpenAI base URL and
                // uses /chat/completions directly. Most other providers put it under /v1.
                if (host.equals("api.deepseek.com", ignoreCase = true)) {
                    "/chat/completions"
                } else {
                    "/v1/chat/completions"
                }
            }

            else -> "$pathWithoutTrailingSlash/chat/completions"
        }

        val authority = uri.rawAuthority
            ?: throw InvalidEndpointException(Error.MALFORMED)
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()
        return "$scheme://$authority$path$query"
    }

    fun isLocal(endpoint: String): Boolean {
        val host = runCatching { URI(normalize(endpoint)).host?.lowercase() }.getOrNull()
        return host in LOCAL_HTTP_HOSTS
    }
}
