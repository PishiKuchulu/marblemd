package com.marblemd.app.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.marblemd.app.BuildConfig
import com.marblemd.app.model.ThemeMode
import com.marblemd.app.update.UpdateStatus
import com.marblemd.app.update.UpdateUiState

/**
 * The app's Settings section: appearance, a shortcut into reading settings,
 * update behaviour for this fork's own releases, and about/links. Reading
 * typography itself stays in the dedicated Reading settings sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSheet(
    themeMode: ThemeMode,
    autoUpdateEnabled: Boolean,
    updateState: UpdateUiState,
    onDismiss: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit,
    onOpenReadingSettings: () -> Unit,
    onOpenUpdates: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Settings", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Appearance, updates and about.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Palette, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Appearance", style = MaterialTheme.typography.titleSmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        AssistChip(
                            onClick = { onThemeModeChange(mode) },
                            label = {
                                Text(if (mode == themeMode) "✓ ${mode.label}" else mode.label)
                            }
                        )
                    }
                }
                Text(
                    "System follows your device. Light and Dark pin the app theme.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.TextFields, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reading settings", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Text size, reading font, direction and your imported fonts.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onOpenReadingSettings) { Text("Open") }
                }
            }

            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.SystemUpdateAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Updates", style = MaterialTheme.typography.titleSmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Automatic update checks", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Check this fork's GitHub Releases when the app starts.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoUpdateEnabled,
                        onCheckedChange = onAutoUpdateChange
                    )
                }
                Text(
                    text = when (updateState.status) {
                        UpdateStatus.AVAILABLE, UpdateStatus.READY ->
                            "Update available: ${updateState.latestVersion ?: ""}".trim()
                        else -> updateState.message
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (updateState.status == UpdateStatus.AVAILABLE ||
                        updateState.status == UpdateStatus.READY
                    ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                OutlinedButton(onClick = onOpenUpdates, modifier = Modifier.fillMaxWidth()) {
                    Text("Check for updates")
                }
            }

            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("About", style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    "MarbleMD ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium
                )
                SettingsLinkRow(
                    label = "This fork — source & releases",
                    url = "https://github.com/PishiKuchulu/marblemd"
                )
                SettingsLinkRow(
                    label = "Upstream MarbleMD by marble098 (MIT)",
                    url = "https://github.com/marble098/marblemd"
                )
                Text(
                    "Forked under the MIT License; the upstream copyright and license text ships in the repository.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsLinkRow(label: String, url: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Outlined.OpenInNew,
            contentDescription = null,
            modifier = Modifier
                .width(18.dp)
                .height(18.dp)
        )
    }
}
