package com.example.expensetrackerpro.presentation.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


import com.example.expensetrackerpro.data.local.entity.Category
import com.example.expensetrackerpro.utils.CategoryIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySection(
    categories: List<Category>, // Updated from List<String>
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onAddCategoryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            color = primaryColor,
            fontWeight = FontWeight.SemiBold
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categories.forEach { category ->
                val isSelected = category.name == selectedCategory

                // 1. Get the specific icon and its persistent color
                val icon = CategoryIcons.icons[category.iconName] ?: Icons.Default.Category
                val iconColor = CategoryIcons.iconColors[category.iconName] ?: primaryColor

                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(category.name) },
                    label = {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            // 2. Apply the persistent category color here
                            tint = iconColor
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedLeadingIconColor = iconColor,
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconColor = iconColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = outlineVariant,
                        selectedBorderColor = Color.Transparent,
                        borderWidth = 1.dp
                    )
                )
            }

            // Custom "Add New" with Dotted Border
            AddNewCategoryChip(onClick = onAddCategoryClicked)
        }
    }
}

@Composable
fun AddNewCategoryChip(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val cornerRadius = 14.dp

    // We use a Box with a minimum touch target to match FilterChip's behavior
    Box(
        modifier = Modifier
            .height(32.dp) // Visual height of the chip
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                // Dotted border logic
                drawRoundRect(
                    color = primary,
                    style = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx())
                )
            }
    ) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            shape = RoundedCornerShape(cornerRadius),
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = primary
                )
                Spacer(Modifier.width(6.dp))

                Text(
                    text = "Add Category",
                    style = MaterialTheme.typography.bodySmall,
                    color = primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

