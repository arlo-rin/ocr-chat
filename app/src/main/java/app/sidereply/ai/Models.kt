package app.sidereply.ai

data class AiConfig(
    val endpoint: String,
    val model: String,
    val apiKey: String,
)

data class ReplySuggestion(
    val style: String,
    val text: String,
)

enum class AiErrorKind {
    BAD_ENDPOINT,
    CLEARTEXT_NOT_ALLOWED,
    AUTH,
    RATE_LIMIT,
    NOT_FOUND,
    INVALID_REQUEST,
    SERVER,
    NETWORK,
    EMPTY_RESPONSE,
    UNKNOWN,
}

class AiException(
    val kind: AiErrorKind,
    cause: Throwable? = null,
    val detail: String? = null,
) : Exception(kind.name, cause)
