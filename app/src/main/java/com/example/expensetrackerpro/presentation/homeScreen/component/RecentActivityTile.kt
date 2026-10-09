package com.example.expensetrackerpro.presentation.homeScreen.component


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.presentation.common.category.CategoryIconTile
import com.example.expensetrackerpro.utils.CurrencyFormatter
import com.example.expensetrackerpro.utils.DateFormatter
import com.example.expensetrackerpro.ui.theme.extendedColors

@Composable
fun RecentActivityTile(
    transaction: Transaction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconTile(icon = transaction.categoryIcon, size = 48.dp)

        Spacer(modifier = Modifier.width(16.dp))

        // 2. Details (Title & Category)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = transaction.categoryName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // 3. Amount & Date
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = CurrencyFormatter.formatSigned(
                    amount = transaction.amount,
                    isIncome = transaction.transactionType == TransactionType.INCOME
                ),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (transaction.transactionType == TransactionType.INCOME)
                    MaterialTheme.extendedColors.income
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = DateFormatter.formatDate(transaction.dateMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}