package app.sidereply.ui.main

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.sidereply.R
import app.sidereply.ai.VisionProviderPreset
import app.sidereply.data.AppPreferences
import app.sidereply.ui.icons.SideReplyIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideReplyApp(
    state: MainUiState,
    snackbarHostState: SnackbarHostState,
    onSelectTab: (MainTab) -> Unit,
    onToggleService: () -> Unit,
    onCopySampleReply: (String) -> Unit,
    onEndpointChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onApplyPreset: (VisionProviderPreset) -> Unit,
    onToggleKeyVisibility: () -> Unit,
    onRequestClearKey: () -> Unit,
    onConfirmClearKey: () -> Unit,
    onDismissClearKey: () -> Unit,
    onSaveSettings: () -> Unit,
    onTestConnection: () -> Unit,
    onWechatOnlyChange: (Boolean) -> Unit,
    onDualAppChange: (Boolean) -> Unit,
    onThemeModeChange: (AppPreferences.ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onOpenRootCommands: () -> Unit,
    onDismissRootCommands: () -> Unit,
    onDismissRootError: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val topBarTitle = when (state.selectedTab) {
        MainTab.HOME -> stringResource(R.string.app_name)
        MainTab.MODEL -> stringResource(R.string.nav_model_title)
        MainTab.SETTINGS -> stringResource(R.string.nav_settings_title)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colors.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        maxLines = 1,
                    )
                },
                actions = {
                    when (state.selectedTab) {
                        MainTab.HOME -> StatusTopPill(
                            running = state.serviceRunning,
                            configuring = state.rootStarting,
                        )
                        MainTab.MODEL -> Surface(
                            modifier = Modifier.padding(end = 14.dp),
                            shape = CircleShape,
                            color = colors.secondaryContainer,
                            contentColor = colors.onSecondaryContainer,
                        ) {
                            Text(
                                text = stringResource(R.string.openai_compatible),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                        MainTab.SETTINGS -> Unit
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    scrolledContainerColor = colors.surfaceContainer,
                    titleContentColor = colors.onSurface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = colors.surfaceContainer,
            ) {
                NavigationBarItem(
                    selected = state.selectedTab == MainTab.HOME,
                    onClick = { onSelectTab(MainTab.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (state.selectedTab == MainTab.HOME) {
                                SideReplyIcons.HomeFilled
                            } else {
                                SideReplyIcons.HomeOutlined
                            },
                            contentDescription = stringResource(R.string.nav_home),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_home)) },
                )
                NavigationBarItem(
                    selected = state.selectedTab == MainTab.MODEL,
                    onClick = { onSelectTab(MainTab.MODEL) },
                    icon = {
                        Icon(
                            imageVector = if (state.selectedTab == MainTab.MODEL) {
                                SideReplyIcons.ModelFilled
                            } else {
                                SideReplyIcons.ModelOutlined
                            },
                            contentDescription = stringResource(R.string.nav_model),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_model)) },
                )
                NavigationBarItem(
                    selected = state.selectedTab == MainTab.SETTINGS,
                    onClick = { onSelectTab(MainTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (state.selectedTab == MainTab.SETTINGS) {
                                SideReplyIcons.TuneFilled
                            } else {
                                SideReplyIcons.TuneOutlined
                            },
                            contentDescription = stringResource(R.string.nav_settings),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_settings)) },
                )
            }
        },
    ) { innerPadding ->
        Crossfade(
            targetState = state.selectedTab,
            animationSpec = tween(180),
            label = "mainTabCrossfade",
        ) { tab ->
            when (tab) {
                MainTab.HOME -> HomeTab(
                    state = state,
                    contentPadding = innerPadding,
                    onToggleService = onToggleService,
                    onWechatOnlyChange = onWechatOnlyChange,
                    onOpenModelTab = { onSelectTab(MainTab.MODEL) },
                    onOpenRootCommands = onOpenRootCommands,
                    onCopySampleReply = onCopySampleReply,
                )
                MainTab.MODEL -> ModelTab(
                    state = state,
                    contentPadding = innerPadding,
                    onEndpointChange = onEndpointChange,
                    onModelChange = onModelChange,
                    onApiKeyChange = onApiKeyChange,
                    onApplyPreset = onApplyPreset,
                    onToggleKeyVisibility = onToggleKeyVisibility,
                    onRequestClearKey = onRequestClearKey,
                    onSave = onSaveSettings,
                    onTestConnection = onTestConnection,
                )
                MainTab.SETTINGS -> SettingsTab(
                    state = state,
                    contentPadding = innerPadding,
                    onWechatOnlyChange = onWechatOnlyChange,
                    onDualAppChange = onDualAppChange,
                    onThemeModeChange = onThemeModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onOpenRootCommands = onOpenRootCommands,
                )
            }
        }
    }

    if (state.showClearKeyDialog) {
        AlertDialog(
            onDismissRequest = onDismissClearKey,
            icon = {
                Icon(
                    imageVector = SideReplyIcons.Delete,
                    contentDescription = null,
                    tint = colors.error,
                )
            },
            title = { Text(stringResource(R.string.clear_key_title)) },
            text = { Text(stringResource(R.string.clear_key_message)) },
            confirmButton = {
                Button(
                    onClick = onConfirmClearKey,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError,
                    ),
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearKey) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (state.rootErrorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissRootError,
            icon = {
                Icon(
                    imageVector = SideReplyIcons.ErrorOutline,
                    contentDescription = null,
                    tint = colors.error,
                )
            },
            title = { Text(stringResource(R.string.root_error_title)) },
            text = { Text(state.rootErrorMessage) },
            confirmButton = {
                Button(onClick = onDismissRootError) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }

    if (state.showRootCommandsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissRootCommands,
            sheetState = sheetState,
            containerColor = colors.surfaceContainerLow,
        ) {
            RootCommandsSheetBody(onDismiss = onDismissRootCommands)
        }
    }
}

@Composable
private fun StatusTopPill(
    running: Boolean,
    configuring: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = when {
            configuring -> colors.tertiaryContainer
            running -> colors.primaryContainer
            else -> colors.surfaceContainerHigh
        },
        label = "pillBg",
    )
    val fg by animateColorAsState(
        targetValue = when {
            configuring -> colors.onTertiaryContainer
            running -> colors.onPrimaryContainer
            else -> colors.onSurfaceVariant
        },
        label = "pillFg",
    )
    val dot by animateColorAsState(
        targetValue = when {
            configuring -> colors.tertiary
            running -> colors.primary
            else -> colors.outline
        },
        label = "pillDot",
    )

    Surface(
        modifier = Modifier.padding(end = 14.dp),
        shape = CircleShape,
        color = bg,
        contentColor = fg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
            Text(
                text = when {
                    configuring -> stringResource(R.string.status_configuring)
                    running -> stringResource(R.string.status_running)
                    else -> stringResource(R.string.status_stopped)
                },
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun RootCommandsSheetBody(onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val commands = listOf(
        Triple(
            stringResource(R.string.root_cmd_1_title),
            "appops set app.sidereply SYSTEM_ALERT_WINDOW allow\nappops set app.sidereply GET_USAGE_STATS allow",
            stringResource(R.string.root_cmd_1_desc),
        ),
        Triple(
            stringResource(R.string.root_cmd_2_title),
            "su -c screencap -p",
            stringResource(R.string.root_cmd_2_desc),
        ),
        Triple(
            stringResource(R.string.root_cmd_3_title),
            "pm list users\ndumpsys window displays | grep -m 2 -E 'mCurrentFocus=|mFocusedApp='",
            stringResource(R.string.root_cmd_3_desc),
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = SideReplyIcons.Terminal,
                contentDescription = null,
                tint = colors.primary,
            )
            Text(
                text = stringResource(R.string.root_commands_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
        }

        Text(
            text = stringResource(R.string.service_restricted_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )

        commands.forEach { (title, cmd, desc) ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceContainerLowest,
                    ) {
                        Text(
                            text = cmd,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = colors.primary,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = CircleShape,
        ) {
            Text(stringResource(R.string.got_it))
        }
    }
}
