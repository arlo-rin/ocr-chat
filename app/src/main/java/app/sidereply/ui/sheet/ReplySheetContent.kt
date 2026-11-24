package app.sidereply.ui.sheet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.sidereply.R
import app.sidereply.ai.ReplySuggestion
import app.sidereply.ui.icons.SideReplyIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val QUICK_STYLES = listOf(
    "更简短",
    "温柔",
    "严谨",
    "幽默",
    "礼貌委婉",
    "直接一点",
)

@Composable
fun ReplySheetContent(
    state: ReplySheetUiState,
    onClose: () -> Unit,
    onCopySuggestion: (String) -> Unit,
    onStyleInputChange: (String) -> Unit,
    onRegenerate: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val windowHeightPx = LocalWindowInfo.current.containerSize.height
    val scope = rememberCoroutineScope()

    val enterProgress = remember { Animatable(0f) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var logExpanded by remember { mutableStateOf(false) }

    // Keep the AI conversation log open while loading so the user sees live progress immediately;
    // once suggestions arrive, keep it neatly toggleable unless multiple rounds exist or user opened it.
    LaunchedEffect(state.phase, state.rounds.size) {
        if (state.phase == SheetPhase.LOADING) {
            logExpanded = true
        } else if (state.phase == SheetPhase.SUGGESTIONS && state.rounds.size == 1) {
            logExpanded = false
        }
    }

    LaunchedEffect(Unit) {
        enterProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    val topInsetDp = with(density) { state.topInsetPx.toDp() }
    val bottomInsetDp = with(density) { state.bottomInsetPx.toDp() }
    val slideOffsetPx = with(density) { ((1f - enterProgress.value) * 72f).dp.toPx() }
    val maxBodyHeight = with(density) { (windowHeightPx * 0.56f).toDp() }.coerceAtLeast(240.dp)
    val dismissThresholdPx = with(density) { 96.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim.copy(alpha = 0.38f * enterProgress.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInsetDp + 24.dp)
                .imePadding()
                .offset { IntOffset(0, (slideOffsetPx + dragOffsetPx).roundToInt()) }
                .alpha(enterProgress.value.coerceIn(0f, 1f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* consume touches inside sheet */ },
                ),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = colors.surfaceContainerLow,
            tonalElevation = 2.dp,
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottomInsetDp + 14.dp),
            ) {
                // Draggable header zone (drag handle + title bar)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragOffsetPx > dismissThresholdPx) {
                                        onClose()
                                    } else {
                                        scope.launch {
                                            val anim = Animatable(dragOffsetPx)
                                            anim.animateTo(
                                                0f,
                                                spring(stiffness = Spring.StiffnessMedium),
                                            ) {
                                                dragOffsetPx = value
                                            }
                                        }
                                    }
                                },
                                onDragCancel = { dragOffsetPx = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    dragOffsetPx = (dragOffsetPx + dragAmount).coerceAtLeast(0f)
                                },
                            )
                        }
                        .padding(horizontal = 20.dp),
                ) {
                    // Standard Material 3 Bottom Sheet Drag Handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(colors.onSurfaceVariant.copy(alpha = 0.4f)),
                        )
                    }

                    // Sheet Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = SideReplyIcons.Sparkle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.sheet_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = colors.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.sheet_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }

                        if (state.rounds.isNotEmpty()) {
                            val latest = state.rounds.last()
                            Surface(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { logExpanded = !logExpanded },
                                shape = CircleShape,
                                color = if (logExpanded) {
                                    colors.secondaryContainer
                                } else {
                                    colors.surfaceContainerHigh
                                },
                                contentColor = if (logExpanded) {
                                    colors.onSecondaryContainer
                                } else {
                                    colors.onSurfaceVariant
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = SideReplyIcons.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.sheet_round_chip,
                                            latest.index,
                                            latest.imageKb,
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                    Icon(
                                        imageVector = if (logExpanded) {
                                            SideReplyIcons.ExpandLess
                                        } else {
                                            SideReplyIcons.ExpandMore
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }

                        FilledTonalIconButton(
                            onClick = onClose,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = colors.surfaceContainerHigh,
                                contentColor = colors.onSurfaceVariant,
                            ),
                        ) {
                            Icon(
                                imageVector = SideReplyIcons.Close,
                                contentDescription = stringResource(R.string.sheet_close),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Scrollable middle content (AI log + loading/error/suggestions)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxBodyHeight)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Expandable Conversation Log with AI
                    AnimatedVisibility(
                        visible = state.rounds.isNotEmpty() && logExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        AiConversationLogCard(rounds = state.rounds)
                    }

                    when (state.phase) {
                        SheetPhase.LOADING -> LoadingSection(
                            message = stringResource(state.loadingMessageRes),
                        )

                        SheetPhase.ERROR -> ErrorSection(
                            message = state.errorMessage,
                            showRetry = state.showRetry,
                            showOpenSettings = state.showOpenSettings,
                            onRetry = onRetry,
                            onOpenSettings = onOpenSettings,
                        )

                        SheetPhase.SUGGESTIONS -> SuggestionsSection(
                            suggestions = state.suggestions,
                            onCopySuggestion = onCopySuggestion,
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                }

                // Ergonomic thumb-zone style refinement bar (only when suggestions are ready)
                AnimatedVisibility(visible = state.phase == SheetPhase.SUGGESTIONS) {
                    StyleRefinementBar(
                        styleInput = state.styleInput,
                        onStyleInputChange = onStyleInputChange,
                        onRegenerate = onRegenerate,
                    )
                }
            }
        }
    }
}

@Composable
private fun AiConversationLogCard(rounds: List<SheetAiRound>) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.sheet_ai_log_title, rounds.last().index),
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
            )

            rounds.forEachIndexed { index, round ->
                val isNewest = index == rounds.lastIndex && !round.pending && !round.failed
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.sheet_round_meta, round.index, round.time),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    val userText = buildString {
                        append(stringResource(R.string.sheet_round_user, round.imageKb))
                        if (round.style.isNotBlank()) {
                            append(" · ")
                            append(stringResource(R.string.sheet_round_style, round.style))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomEnd = 4.dp,
                                bottomStart = 16.dp,
                            ),
                            color = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                        ) {
                            Text(
                                text = userText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }

                    val aiText = when {
                        round.pending -> stringResource(R.string.sheet_round_pending)
                        round.failed -> stringResource(R.string.sheet_round_failed)
                        isNewest -> stringResource(R.string.sheet_round_done)
                        else -> round.suggestions.take(3).joinToString("\n") { "${it.style}：${it.text}" }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 32.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomEnd = 16.dp,
                                bottomStart = 4.dp,
                            ),
                            color = if (round.failed) colors.errorContainer else colors.surfaceContainerLowest,
                            contentColor = if (round.failed) colors.onErrorContainer else colors.onSurface,
                        ) {
                            Text(
                                text = aiText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingSection(message: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colors.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp,
                strokeCap = StrokeCap.Round,
                color = colors.primary,
                trackColor = colors.primaryContainer,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(4.dp)
                    .clip(CircleShape),
                strokeCap = StrokeCap.Round,
                color = colors.primary,
                trackColor = colors.surfaceContainerHighest,
            )
        }
    }
}

@Composable
private fun ErrorSection(
    message: String,
    showRetry: Boolean,
    showOpenSettings: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colors.errorContainer,
        contentColor = colors.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = SideReplyIcons.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.sheet_error_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onErrorContainer.copy(alpha = 0.9f),
            )

            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (showRetry) {
                    Button(
                        onClick = onRetry,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.error,
                            contentColor = colors.onError,
                        ),
                    ) {
                        Icon(
                            imageVector = SideReplyIcons.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.sheet_retry))
                    }
                }
                if (showOpenSettings) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, colors.onErrorContainer.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.onErrorContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = SideReplyIcons.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.sheet_open_settings))
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionsSection(
    suggestions: List<ReplySuggestion>,
    onCopySuggestion: (String) -> Unit,
) {
    var copiedIndex by remember(suggestions) { mutableIntStateOf(-1) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(copiedIndex) {
        if (copiedIndex >= 0) {
            delay(1400)
            copiedIndex = -1
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        suggestions.take(3).forEachIndexed { index, suggestion ->
            SuggestionCard(
                index = index,
                suggestion = suggestion,
                copied = copiedIndex == index,
                onCopy = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    copiedIndex = index
                    onCopySuggestion(suggestion.text)
                },
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    index: Int,
    suggestion: ReplySuggestion,
    copied: Boolean,
    onCopy: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val (tagBg, tagFg) = when (index % 3) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> colors.secondaryContainer to colors.onSecondaryContainer
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onCopy),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerLowest,
        border = BorderStroke(
            width = 1.dp,
            color = if (copied) colors.primary else colors.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = CircleShape,
                    color = tagBg,
                    contentColor = tagFg,
                ) {
                    Text(
                        text = suggestion.style,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                AnimatedContent(
                    targetState = copied,
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                    label = "copyPill",
                ) { isCopied ->
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onCopy),
                        shape = CircleShape,
                        color = if (isCopied) colors.primary else colors.surfaceContainerHigh,
                        contentColor = if (isCopied) colors.onPrimary else colors.primary,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = if (isCopied) SideReplyIcons.Check else SideReplyIcons.Copy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = stringResource(if (isCopied) R.string.sheet_copied else R.string.sheet_copy),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }

            Text(
                text = suggestion.text,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
            )
        }
    }
}

@Composable
private fun StyleRefinementBar(
    styleInput: String,
    onStyleInputChange: (String) -> Unit,
    onRegenerate: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(
            color = colors.outlineVariant.copy(alpha = 0.45f),
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        // One-tap tone FilterChips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(QUICK_STYLES) { style ->
                val selected = styleInput.trim() == style
                FilterChip(
                    selected = selected,
                    onClick = {
                        onStyleInputChange(if (selected) "" else style)
                    },
                    label = {
                        Text(
                            text = style,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = SideReplyIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }

        // Style input + Regenerate pill row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = styleInput,
                onValueChange = { next ->
                    if (next.length <= 40) onStyleInputChange(next)
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = stringResource(R.string.style_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = colors.surfaceContainerLowest,
                    focusedContainerColor = colors.surfaceContainerLowest,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onRegenerate()
                    },
                ),
            )

            FilledTonalButton(
                onClick = {
                    focusManager.clearFocus()
                    onRegenerate()
                },
                modifier = Modifier.height(52.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Icon(
                    imageVector = SideReplyIcons.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.sheet_regenerate),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Text(
            text = stringResource(R.string.sheet_footer_m3),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
        )
    }
}
