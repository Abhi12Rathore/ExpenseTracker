package com.example.expensetrackerpro.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.ui.theme.extendedColors

/** A [SettingsItem] that ends in a switch. */
@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: Color,
    enabled: Boolean = true
) {
    SettingsItem(
        icon = icon,
        iconTint = iconTint,
        title = title,
        subtitle = subtitle,
        // The whole row is the switch, so the Switch itself takes no clicks
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange
        ),
        enabled = enabled,
        trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) }
    )
}

@PreviewLightDark
@Composable
private fun SettingsSwitchItemPreview() {
    ExpenseTrackerProTheme {
        Surface {
            Column {
                SettingsSwitchItem(
                    icon = Icons.Default.Lock,
                    title = "App lock",
                    subtitle = "Unlock with biometrics or screen lock",
                    checked = true,
                    onCheckedChange = {},
                    iconTint = MaterialTheme.extendedColors.categoryPalette.blueGrey
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Lock,
                    title = "App lock",
                    subtitle = "Unlock with biometrics or screen lock",
                    checked = false,
                    onCheckedChange = {},
                    iconTint = MaterialTheme.extendedColors.categoryPalette.blueGrey
                )
                SettingsSwitchItem(
                    icon = Icons.Default.Lock,
                    title = "App lock",
                    subtitle = "Set a screen lock in your device settings to use app lock",
                    checked = false,
                    onCheckedChange = {},
                    iconTint = MaterialTheme.extendedColors.categoryPalette.blueGrey,
                    enabled = false
                )
            }
        }
    }
}
