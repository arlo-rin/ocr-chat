package app.sidereply.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.sidereply.R
import app.sidereply.ai.ProviderPresets
import app.sidereply.ai.VisionProviderPreset
import app.sidereply.ui.icons.SideReplyIcons

@Composable
fun ModelTab(
    state: MainUiState,
    contentPadding: PaddingValues,
    onEndpointChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onApplyPreset: (VisionProviderPreset) -> Unit,
    onToggleKeyVisibility: () -> Unit,
    onRequestClearKey: () -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val matchedPresetId = ProviderPresets.matchingId(state.endpoint)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Vision Provider Quick Presets
        item(key = "presets") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.model_presets_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ProviderPresets.ALL, key = { it.id }) { preset ->
                        val selected = preset.id == matchedPresetId
                        FilterChip(
                            selected = selected,
                            onClick = { onApplyPreset(preset) },
                            label = { Text(preset.label) },
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
            }
        }

        // Main Configuration Card
        item(key = "form_card") {
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
                        text = stringResource(R.string.ai_section_desc_m3),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = state.endpoint,
                        onValueChange = onEndpointChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.label_endpoint)) },
                        placeholder = { Text(stringResource(R.string.hint_endpoint_m3)) },
                        leadingIcon = {
                            Icon(
                                imageVector = SideReplyIcons.Link,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        trailingIcon = if (state.endpoint.isNotEmpty()) {
                            {
                                IconButton(onClick = { onEndpointChange("") }) {
                                    Icon(
                                        imageVector = SideReplyIcons.Close,
                                        contentDescription = stringResource(R.string.clear),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        isError = state.endpointError != null,
                        supportingText = {
                            Text(
                                text = state.endpointError
                                    ?: stringResource(R.string.endpoint_supporting_hint),
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    OutlinedTextField(
                        value = state.model,
                        onValueChange = onModelChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.label_model)) },
                        placeholder = { Text(stringResource(R.string.hint_model_m3)) },
                        leadingIcon = {
                            Icon(
                                imageVector = SideReplyIcons.Sparkle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        isError = state.modelError != null,
                        supportingText = {
                            Text(
                                text = state.modelError
                                    ?: stringResource(R.string.model_supporting_hint),
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    OutlinedTextField(
                        value = state.apiKeyInput,
                        onValueChange = onApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.label_api_key)) },
                        placeholder = {
                            Text(
                                text = if (state.hasSavedKey) {
                                    stringResource(R.string.api_key_saved_placeholder)
                                } else {
                                    stringResource(R.string.hint_api_key)
                                },
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = SideReplyIcons.Key,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = onToggleKeyVisibility) {
                                Icon(
                                    imageVector = if (state.keyVisible) {
                                        SideReplyIcons.VisibilityOff
                                    } else {
                                        SideReplyIcons.Visibility
                                    },
                                    contentDescription = stringResource(
                                        if (state.keyVisible) R.string.hide_key else R.string.show_key,
                                    ),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        visualTransformation = if (state.keyVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        supportingText = {
                            Text(
                                text = if (state.hasSavedKey) {
                                    stringResource(R.string.api_key_saved_hint, state.savedKeyTail)
                                } else {
                                    stringResource(R.string.api_key_empty_hint)
                                },
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onSave()
                            },
                        ),
                    )

                    if (state.hasSavedKey) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = onRequestClearKey,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = colors.error,
                                ),
                            ) {
                                Icon(
                                    imageVector = SideReplyIcons.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.clear_saved_key))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onSave()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = CircleShape,
                        ) {
                            Icon(
                                imageVector = SideReplyIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.save_settings),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                focusManager.clearFocus()
                                onTestConnection()
                            },
                            enabled = !state.testing,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = CircleShape,
                        ) {
                            if (state.testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.test_testing_short),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            } else {
                                Icon(
                                    imageVector = SideReplyIcons.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.test_connection),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = !state.inlineMessage.isNullOrBlank()) {
                        val isError = state.inlineMessageIsError
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isError) colors.errorContainer else colors.secondaryContainer,
                            contentColor = if (isError) colors.onErrorContainer else colors.onSecondaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = if (isError) {
                                        SideReplyIcons.ErrorOutline
                                    } else {
                                        SideReplyIcons.CheckCircle
                                    },
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(18.dp),
                                )
                                Text(
                                    text = state.inlineMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Vision Model Reminder Card
        item(key = "vision_note") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = colors.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = SideReplyIcons.Shield,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(20.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.vision_tip_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.vision_tip_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
