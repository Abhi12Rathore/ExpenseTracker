package com.example.expensetrackerpro.presentation.common.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryIconGroup

private const val BACKGROUND_ALPHA = 0.15f

/**
 * A category's icon as it appears everywhere: in its hue, on a rounded square washed with the
 * same hue. The icon is decoration here; whatever names the category sits next to it.
 */
@Composable
fun CategoryIconTile(
    icon: CategoryIcon,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    // Set where the tile stands alone, as in the icon picker
    contentDescription: String? = null
) {
    val color = icon.color

    Box(
        modifier = modifier
            .size(size)
            // Corners and icon scale with the tile, so every size keeps the same proportions
            .background(color.copy(alpha = BACKGROUND_ALPHA), RoundedCornerShape(size * 0.27f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon.ui.imageVector,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(size / 2)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@PreviewLightDark
@Composable
private fun CategoryIconGalleryPreview() {
    ExpenseTrackerProTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryIconGroup.entries.forEach { group ->
                    Text(
                        text = stringResource(group.labelRes()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIcon.inGroup(group).forEach { icon -> CategoryIconTile(icon = icon) }
                    }
                }
            }
        }
    }
}
