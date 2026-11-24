package app.sidereply.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sidereply.R
import app.sidereply.ui.icons.SideReplyIcons
import kotlinx.coroutines.delay

private data class SampleReply(
    val tone: String,
    val reply: String,
)

private val HOME_PREVIEW_REPLIES = listOf(
    SampleReply("自然", "好呀，周六晚上怎么样？地方你来挑。"),
    SampleReply("稳妥", "周六晚上应该有空，具体时间地点你定了发我。"),
    SampleReply("轻松", "必须有空呀，这周就指着这顿饭续命了！"),
)

@Composable
fun HomeTab(
    state: MainUiState,
    contentPadding: PaddingValues,
    onToggleService: () -> Unit,
    onOpenModelTab: () -> Unit,
    onOpenRootCommands: () -> Unit,
    onCopySampleReply: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "hero_control") {
            ServiceHeroCard(
                state = state,
                onToggleService = onToggleService,
            )
        }

        item(key = "status_bento") {
            StatusBentoRow(
                state = state,
                onOpenRootCommands = onOpenRootCommands,
                onOpenModelTab = onOpenModelTab,
            )
        }

        item(key = "interactive_preview") {
            InteractivePreviewCard(
                onCopySampleReply = onCopySampleReply,
            )
        }

        item(key = "workflow_steps") {
            WorkflowGuideCard()
        }
    }
}

@Composable
private fun ServiceHeroCard(
    state: MainUiState,
    onToggleService: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = when {
            state.rootStarting -> colors.tertiaryContainer
            state.serviceRunning -> colors.primaryContainer
            else -> colors.surfaceContainerHigh
        },
        animationSpec = tween(260),
        label = "heroContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            state.rootStarting -> colors.onTertiaryContainer
            state.serviceRunning -> colors.onPrimaryContainer
            else -> colors.onSurface
        },
        animationSpec = tween(260),
        label = "heroContent",
    )
    val badgeBg by animateColorAsState(
        targetValue = when {
            state.rootStarting -> colors.tertiary
            state.serviceRunning -> colors.primary
            else -> colors.surfaceContainerLowest
        },
        animationSpec = tween(260),
        label = "heroBadgeBg",
    )
    val badgeFg by animateColorAsState(
        targetValue = when {
            state.rootStarting -> colors.onTertiary
            state.serviceRunning -> colors.onPrimary
            else -> colors.primary
        },
        animationSpec = tween(260),
        label = "heroBadgeFg",
    )

    val title = when {
        state.rootStarting -> stringResource(R.string.root_starting)
        state.serviceRunning -> stringResource(R.string.service_on_title)
        else -> stringResource(R.string.service_off_title)
    }
    val subtitle = when {
        state.rootStarting -> stringResource(R.string.root_waiting_for_grant)
        state.serviceRunning -> stringResource(R.string.service_state_running)
        !state.overlayGranted && !state.usageGranted -> stringResource(R.string.service_state_needs_root)
        !state.overlayGranted -> stringResource(R.string.service_permission_overlay)
        !state.usageGranted -> stringResource(R.string.service_permission_usage)
        else -> stringResource(R.string.service_state_ready)
    }
    val description = when {
        state.serviceRunning -> stringResource(R.string.service_on_desc)
        else -> stringResource(R.string.service_off_desc)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = badgeBg,
                    contentColor = badgeFg,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.serviceRunning) SideReplyIcons.Sparkle else SideReplyIcons.Power,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = contentColor,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.78f),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.86f),
            )

            AnimatedVisibility(visible = state.rootStarting) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = colors.tertiary,
                        trackColor = colors.onTertiaryContainer.copy(alpha = 0.14f),
                        strokeCap = StrokeCap.Round,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            if (state.serviceRunning) {
                FilledTonalButton(
                    onClick = onToggleService,
                    enabled = !state.rootStarting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colors.surfaceContainerLowest.copy(alpha = 0.92f),
                        contentColor = colors.onSurface,
                    ),
                ) {
                    Icon(
                        imageVector = SideReplyIcons.StopCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.overlay_stop),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                Button(
                    onClick = onToggleService,
                    enabled = !state.rootStarting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                ) {
                    if (state.rootStarting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.2.dp,
                            color = colors.onSurface.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.root_starting),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    } else {
                        Icon(
                            imageVector = SideReplyIcons.Power,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.overlay_start),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBentoRow(
    state: MainUiState,
    onOpenRootCommands: () -> Unit,
    onOpenModelTab: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Permissions & Root card
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onOpenRootCommands),
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = SideReplyIcons.Terminal,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.bento_permissions_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                    )
                }

                Spacer(Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PermissionDotRow(
                        label = stringResource(R.string.bento_perm_overlay),
                        granted = state.overlayGranted,
                    )
                    PermissionDotRow(
                        label = stringResource(R.string.bento_perm_usage),
                        granted = state.usageGranted,
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.bento_root_tap_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                )
            }
        }

        // AI Engine card
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onOpenModelTab),
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = if (state.isAiReady) colors.tertiaryContainer else colors.errorContainer,
                        contentColor = if (state.isAiReady) colors.onTertiaryContainer else colors.onErrorContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.isAiReady) SideReplyIcons.ModelFilled else SideReplyIcons.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.bento_ai_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                    )
                }

                Spacer(Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = state.model.ifBlank { stringResource(R.string.bento_ai_not_set) },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (state.isAiReady) {
                            state.savedEndpointHost
                        } else {
                            stringResource(R.string.bento_ai_needs_key)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isAiReady) colors.onSurfaceVariant else colors.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.bento_ai_tap_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                )
            }
        }
    }
}

@Composable
private fun PermissionDotRow(
    label: String,
    granted: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (granted) colors.primary else colors.outlineVariant),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(if (granted) R.string.bento_granted else R.string.bento_pending),
            style = MaterialTheme.typography.labelSmall,
            color = if (granted) colors.primary else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun InteractivePreviewCard(
    onCopySampleReply: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    var selectedToneIndex by remember { mutableIntStateOf(0) }
    var copiedSample by remember { mutableStateOf(false) }

    LaunchedEffect(copiedSample) {
        if (copiedSample) {
            delay(1400)
            copiedSample = false
        }
    }

    val activeSample = HOME_PREVIEW_REPLIES[selectedToneIndex]

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.home_preview_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.home_preview_interactive_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Simulated incoming message bubble
            Text(
                text = stringResource(R.string.preview_peer),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            Surface(
                shape = RoundedCornerShape(
                    topStart = 6.dp,
                    topEnd = 18.dp,
                    bottomEnd = 18.dp,
                    bottomStart = 18.dp,
                ),
                color = colors.surfaceContainerHighest,
            ) {
                Text(
                    text = stringResource(R.string.hero_preview_incoming),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            // Interactive M3 FilterChips to switch sample tone
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HOME_PREVIEW_REPLIES.forEachIndexed { index, item ->
                    val selected = index == selectedToneIndex
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedToneIndex = index
                            copiedSample = false
                        },
                        label = {
                            Text(
                                text = item.tone,
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

            Spacer(Modifier.height(8.dp))

            // Simulated AI reply suggestion
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        copiedSample = true
                        onCopySampleReply(activeSample.reply)
                    },
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceContainerLowest,
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.22f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = SideReplyIcons.Sparkle,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "${stringResource(R.string.hero_preview_label)} · ${activeSample.tone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        AnimatedContent(
                            targetState = activeSample.reply,
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                            label = "sampleReply",
                        ) { replyText ->
                            Text(
                                text = replyText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurface,
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            copiedSample = true
                            onCopySampleReply(activeSample.reply)
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (copiedSample) colors.primary else colors.primaryContainer,
                            contentColor = if (copiedSample) colors.onPrimary else colors.onPrimaryContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = if (copiedSample) SideReplyIcons.Check else SideReplyIcons.Copy,
                            contentDescription = stringResource(R.string.sheet_copy),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkflowGuideCard() {
    val colors = MaterialTheme.colorScheme
    val steps = listOf(
        stringResource(R.string.guide_step_1_title) to stringResource(R.string.guide_step_1_desc),
        stringResource(R.string.guide_step_2_title) to stringResource(R.string.guide_step_2_desc),
        stringResource(R.string.guide_step_3_title) to stringResource(R.string.guide_step_3_desc),
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.guide_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )

            steps.forEachIndexed { idx, (title, desc) ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
