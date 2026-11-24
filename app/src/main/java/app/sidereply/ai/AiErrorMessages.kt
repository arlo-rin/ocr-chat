package app.sidereply.ai

import android.content.Context
import app.sidereply.R

/** Maps transport errors to short, actionable Chinese messages for both entry points. */
object AiErrorMessages {

    fun forThrowable(context: Context, throwable: Throwable): String {
        val error = throwable as? AiException
        val base = when (error?.kind) {
            AiErrorKind.BAD_ENDPOINT -> context.getString(R.string.ai_error_bad_endpoint)
            AiErrorKind.CLEARTEXT_NOT_ALLOWED -> context.getString(R.string.ai_error_cleartext)
            AiErrorKind.AUTH -> context.getString(R.string.ai_error_auth)
            AiErrorKind.RATE_LIMIT -> context.getString(R.string.ai_error_rate_limit)
            AiErrorKind.NOT_FOUND -> context.getString(R.string.ai_error_not_found)
            AiErrorKind.INVALID_REQUEST -> context.getString(R.string.ai_error_invalid_request)
            AiErrorKind.SERVER -> context.getString(R.string.ai_error_server)
            AiErrorKind.NETWORK -> context.getString(R.string.ai_error_network)
            AiErrorKind.EMPTY_RESPONSE -> context.getString(R.string.ai_error_empty)
            AiErrorKind.UNKNOWN, null -> context.getString(R.string.ai_error_generic)
        }

        val detail = error?.detail?.trim()?.take(220)
        val detailIsSafeToShow = error?.kind == AiErrorKind.INVALID_REQUEST ||
            error?.kind == AiErrorKind.EMPTY_RESPONSE ||
            error?.kind == AiErrorKind.UNKNOWN
        return if (!detail.isNullOrBlank() && detailIsSafeToShow) {
            base + "\n" + context.getString(R.string.ai_error_detail, detail)
        } else {
            base
        }
    }
}
