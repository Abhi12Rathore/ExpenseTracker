package com.example.expensetrackerpro.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.ui.theme.extendedColors

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The whole row is the switch, so the Switch itself takes no clicks
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            // Stays at full strength when disabled: it is what explains why
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
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
