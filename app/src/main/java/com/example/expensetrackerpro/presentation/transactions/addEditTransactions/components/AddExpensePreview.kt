package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.utils.CurrencyFormatter
import com.example.expensetrackerpro.utils.DateFormatter

@Composable
fun AddExpensePreview(
    amount: String,
    category: String,
    dateMillis: Long,
    type: TransactionType,
    modifier: Modifier = Modifier
) {
    val isIncome = type == TransactionType.INCOME
    val amountColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Transaction Preview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                // Income is +, Expense is -
                text = CurrencyFormatter.formatSigned(amount.toDoubleOrNull() ?: 0.0, isIncome),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = amountColor
            )
            Text(
                text = "$category • ${DateFormatter.formatDate(dateMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
