package com.example.expensetrackerpro.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.ui.theme.extendedColors

// Material's opacity for disabled content
private const val DISABLED_ALPHA = 0.38f
private const val ICON_TILE_ALPHA = 0.15f
private val IconTileSize = 40.dp
private val IconTileShape = RoundedCornerShape(12.dp)
private val CardShape = RoundedCornerShape(20.dp)

/** A titled group of settings on one card. Rows are separated with [SettingsDivider]. */
@Composable
fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    // The danger zone names itself in the error color
    titleColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = titleColor,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .semantics { heading() }
        )
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

/** Starts under the text, past the icon tile. */
@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * The one row every setting is built from: an icon on a tile of its own tint, a title and a
 * subtitle, then whatever the setting needs at the end of the row or underneath it.
 */
@Composable
fun SettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    // Null for a row that is not tapped as a whole
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null
) {
    val contentAlpha = if (enabled) 1f else DISABLED_ALPHA

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(IconTileSize)
                    .alpha(contentAlpha)
                    .background(iconTint.copy(alpha = ICON_TILE_ALPHA), IconTileShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = titleColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.alpha(contentAlpha)
                )
                // Stays at full strength when disabled: it is what explains why
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing?.invoke()
        }
        below?.invoke()
    }
}

/** Marks a setting that is not available yet. */
@Composable
fun SoonPill(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.settings_soon),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsSectionCardPreview() {
    ExpenseTrackerProTheme {
        Surface {
            SettingsSectionCard(title = "Data", modifier = Modifier.padding(16.dp)) {
                SettingsItem(
                    icon = Icons.Default.Timer,
                    iconTint = MaterialTheme.extendedColors.categoryPalette.indigo,
                    title = "Lock after",
                    subtitle = "30 seconds",
                    onClick = {}
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.CloudUpload,
                    iconTint = MaterialTheme.extendedColors.categoryPalette.blue,
                    title = "Export & backup",
                    subtitle = "Save a copy of your data",
                    enabled = false,
                    trailing = { SoonPill() }
                )
            }
        }
    }
}
