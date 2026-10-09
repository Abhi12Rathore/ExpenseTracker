package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SaveExpenseButton(
    isEnabled: Boolean,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonLabel:String
) {
    // Animate the button color transition
    val containerColor by animateColorAsState(
        targetValue = if (isEnabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 500),
        label = "ButtonColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isEnabled) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.outline,
        animationSpec = tween(durationMillis = 500),
        label = "ContentColor"
    )

    Button(
        onClick = onSaveClick,
        enabled = isEnabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor, // Managed by animation
            disabledContentColor = contentColor      // Managed by animation
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Requirement: Animated Lock/Unlock Icon
            AnimatedContent(
                targetState = isEnabled,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                label = "LockAnimation"
            ) { targetEnabled ->
                Icon(
                    imageVector = if (targetEnabled) Icons.Default.LockOpen
                    else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = buttonLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}