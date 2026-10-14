package com.example.expensetrackerpro.presentation.transactions.transactionList.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.DayLabel
import com.example.expensetrackerpro.domain.models.TransactionDayGroup
import com.example.expensetrackerpro.presentation.common.currency.LocalCurrencyFormatter
import com.example.expensetrackerpro.ui.theme.extendedColors
import com.example.expensetrackerpro.presentation.common.date.LocalDateFormatter

/** `TODAY · FRI 9 OCT` on the left, the day's net on the right. Opaque, so rows pass under it. */
@Composable
fun DayHeader(day: TransactionDayGroup, modifier: Modifier = Modifier) {
    val currencyFormatter = LocalCurrencyFormatter.current
    val date = LocalDateFormatter.current.formatDayHeader(day.dayStartMillis)
    val title = when (day.label) {
        DayLabel.TODAY -> stringResource(
            R.string.transactions_day_header_named,
            stringResource(R.string.transaction_date_today),
            date
        )
        DayLabel.YESTERDAY -> stringResource(
            R.string.transactions_day_header_named,
            stringResource(R.string.transaction_date_yesterday),
            date
        )
        DayLabel.OTHER -> date
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = when {
                day.net.isZero -> currencyFormatter.format(day.net)
                else -> currencyFormatter.formatSigned(day.net, isIncome = day.net.isPositive)
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                day.net.isPositive -> MaterialTheme.extendedColors.income
                day.net.isNegative -> MaterialTheme.extendedColors.expense
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
