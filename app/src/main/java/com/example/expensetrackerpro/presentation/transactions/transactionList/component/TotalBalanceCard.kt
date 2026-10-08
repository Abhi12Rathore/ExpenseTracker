package com.example.expensetrackerpro.presentation.transactions.transactionList.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.utils.CurrencyFormatter

@Composable
fun TotalBalanceCard(
    totalBalance: Double,   // (Income - Expense)
    totalIncome: Double,    // Sum of Income
    totalExpense: Double    // Sum of Expenses
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // ... Header with Filter Dropdown ...

            // Net Balance (The main big number)
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = CurrencyFormatter.format(totalBalance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Split View: Income and Expense
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Income Summary
                BalanceSummaryItem(
                    label = "Income",
                    amount = totalIncome,
                    icon = Icons.Default.ArrowUpward,
                    // Financial Green
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Expense Summary
                BalanceSummaryItem(
                    label = "Expenses",
                    amount = totalExpense,
                    icon = Icons.Default.ArrowDownward,
                    // Financial Red (from your theme error color)
                    tint = Color.Red,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun BalanceSummaryItem(
    label: String,
    amount: Double,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(tint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = tint)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Black)
            Text(CurrencyFormatter.format(amount), style = MaterialTheme.typography.titleMedium, color =tint, fontWeight = FontWeight.Bold)
        }
    }
}