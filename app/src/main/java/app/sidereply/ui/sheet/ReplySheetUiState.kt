package app.sidereply.ui.sheet

import app.sidereply.R
import app.sidereply.ai.ReplySuggestion
import app.sidereply.data.AppPreferences

enum class SheetPhase {
    LOADING,
    SUGGESTIONS,
    ERROR,
}

data class SheetAiRound(
    val index: Int,
    val imageKb: Int,
    val style: String,
    val suggestions: List<ReplySuggestion>,
    val time: String,
    val pending: Boolean = false,
    val failed: Boolean = false,
)

data class ReplySheetUiState(
    val phase: SheetPhase = SheetPhase.LOADING,
    val loadingMessageRes: Int = R.string.sheet_generating,
    val rounds: List<SheetAiRound> = emptyList(),
    val suggestions: List<ReplySuggestion> = emptyList(),
    val styleInput: String = "",
    val errorMessage: String = "",
    val showRetry: Boolean = false,
    val showOpenSettings: Boolean = false,
    val themeMode: AppPreferences.ThemeMode = AppPreferences.ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val topInsetPx: Int = 0,
    val bottomInsetPx: Int = 0,
)
