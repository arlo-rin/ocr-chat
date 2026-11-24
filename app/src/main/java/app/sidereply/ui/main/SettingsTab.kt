package app.sidereply.ui.main

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sidereply.BuildConfig
import app.sidereply.R
import app.sidereply.data.AppPreferences
import app.sidereply.ui.icons.SideReplyIcons
import app.sidereply.ui.theme.GroupedCardShapes

@Composable
fun SettingsTab(
    state: MainUiState,
    contentPadding: PaddingValues,
    onWechatOnlyChange: (Boolean) -> Unit,
    onDualAppChange: (Boolean) -> Unit,
    onThemeModeChange: (AppPreferences.ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onOpenRootCommands: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Group 1: Floating Bubble Scope
        item(key = "scope_group") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionHeader(title = stringResource(R.string.settings_section_scope))
                Spacer(Modifier.height(6.dp))

                SwitchPreferenceTile(
                    shape = GroupedCardShapes.Top,
                    icon = SideReplyIcons.ChatBubble,
                    title = stringResource(R.string.wechat_only_title),
                    subtitle = stringResource(R.string.wechat_only_desc),
                    checked = state.wechatOnly,
                    onCheckedChange = onWechatOnlyChange,
                )
                SwitchPreferenceTile(
                    shape = GroupedCardShapes.Bottom,
                    icon = SideReplyIcons.DualApps,
                    title = stringResource(R.string.dual_app_title),
                    subtitle = stringResource(R.string.dual_app_desc),
                    checked = state.dualAppSupport,
                    onCheckedChange = onDualAppChange,
                )
            }
        }

        // Group 2: Material You & Appearance
        item(key = "appearance_group") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionHeader(title = stringResource(R.string.settings_section_appearance))
                Spacer(Modifier.height(6.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = if (supportsDynamicColor) GroupedCardShapes.Top else GroupedCardShapes.Single,
                    color = colors.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.theme_mode_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )

                        val options = listOf(
                            AppPreferences.ThemeMode.SYSTEM to stringResource(R.string.theme_mode_system),
                            AppPreferences.ThemeMode.LIGHT to stringResource(R.string.theme_mode_light),
                            AppPreferences.ThemeMode.DARK to stringResource(R.string.theme_mode_dark),
                        )
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            options.forEachIndexed { index, (mode, label) ->
                                SegmentedButton(
                                    selected = state.themeMode == mode,
                                    onClick = { onThemeModeChange(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = options.size,
                                    ),
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }

                if (supportsDynamicColor) {
                    SwitchPreferenceTile(
                        shape = GroupedCardShapes.Bottom,
                        icon = SideReplyIcons.Palette,
                        title = stringResource(R.string.dynamic_color_title),
                        subtitle = stringResource(R.string.dynamic_color_desc),
                        checked = state.dynamicColor,
                        onCheckedChange = onDynamicColorChange,
                    )
                }
            }
        }

        // Group 3: Privacy & Root Transparency
        item(key = "privacy_group") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionHeader(title = stringResource(R.string.settings_section_privacy))
                Spacer(Modifier.height(6.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(GroupedCardShapes.Top)
                        .clickable(onClick = onOpenRootCommands),
                    shape = GroupedCardShapes.Top,
                    color = colors.surfaceContainerLow,
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LeadingIconBadge(icon = SideReplyIcons.Terminal)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.root_commands_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.root_commands_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = SideReplyIcons.ChevronRight,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = GroupedCardShapes.Bottom,
                    color = colors.surfaceContainerLow,
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LeadingIconBadge(icon = SideReplyIcons.Shield)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.privacy_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.privacy_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Footer
        item(key = "footer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.surfaceContainer,
                ) {
                    Text(
                        text = stringResource(R.string.footer_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 6.dp),
    )
}

@Composable
private fun LeadingIconBadge(icon: ImageVector) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SwitchPreferenceTile(
    shape: Shape,
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onCheckedChange(!checked) },
        shape = shape,
        color = colors.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LeadingIconBadge(icon = icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = SideReplyIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}
