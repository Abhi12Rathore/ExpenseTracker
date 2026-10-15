package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.presentation.common.currency.LocalCurrencyFormatter
import com.example.expensetrackerpro.presentation.common.currency.amountFormatKey
import com.example.expensetrackerpro.ui.theme.extendedColors

/** Income green or expense red, from the theme. */
@Composable
fun TransactionType.amountColor(): Color =
    if (this == TransactionType.INCOME) MaterialTheme.extendedColors.income
    else MaterialTheme.extendedColors.expense

/** The large centered amount field: rupee prefix, grouping as you type, decimal keypad. */
@Composable
fun AmountHero(
    amount: String,
    transactionType: TransactionType,
    onAmountChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    isFromBill: Boolean = false,
    fillTag: String = stringResource(R.string.bill_fill_tag)
) {
    val color = transactionType.amountColor()
    val textStyle = MaterialTheme.typography.displaySmall.copy(
        color = color,
        fontWeight = FontWeight.Bold
    )
    val description = stringResource(R.string.transaction_amount)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AmountField(amount, onAmountChange, focusRequester, textStyle, color, description)
        if (isFromBill) FromBillTag(text = fillTag)
    }
}

@Composable
private fun AmountField(
    amount: String,
    onAmountChange: (String) -> Unit,
    focusRequester: FocusRequester,
    textStyle: TextStyle,
    color: Color,
    description: String
) {
    val currencyFormatter = LocalCurrencyFormatter.current
    // Asks the formatter on every edit; a new one when the currency or language changes redraws the field
    val formatKey = amountFormatKey()
    val groupingTransformation = remember(currencyFormatter, formatKey) { AmountGroupingTransformation(currencyFormatter) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Where the app's language writes it: `€12` in English, `12 €` in German
        val symbolAfter = currencyFormatter.isSymbolAfter
        if (!symbolAfter) Text(text = currencyFormatter.symbol, style = textStyle)
        BasicTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier
                // Only as wide as the digits, so symbol and amount stay centered together
                .width(IntrinsicSize.Min)
                .focusRequester(focusRequester)
                .semantics { contentDescription = description },
            textStyle = textStyle,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            visualTransformation = groupingTransformation,
            cursorBrush = SolidColor(color),
            decorationBox = { innerTextField ->
                Box {
                    if (amount.isEmpty()) {
                        Text(
                            text = stringResource(R.string.transaction_amount_placeholder),
                            style = textStyle,
                            color = color.copy(alpha = 0.4f)
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (symbolAfter) {
            Text(text = currencyFormatter.symbol, style = textStyle, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** One line standing in for [AmountHero] while the keyboard is open for another field. */
@Composable
fun AmountSummary(
    amount: Money?,
    transactionType: TransactionType,
    categoryName: String?,
    dateLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = LocalCurrencyFormatter.current
    val amountText = currencyFormatter.format(amount ?: Money.ZERO)
    val summary = stringResource(
        R.string.transaction_summary,
        amountText,
        categoryName ?: stringResource(R.string.transaction_summary_no_category),
        dateLabel
    )
    val amountStyle = SpanStyle(color = transactionType.amountColor(), fontWeight = FontWeight.Bold)

    val editAmountLabel = stringResource(R.string.transaction_summary_edit_amount)

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            // Tells screen readers what tapping the summary is for
            .semantics {
                onClick(label = editAmountLabel) {
                    onClick()
                    true
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Text(
            text = buildAnnotatedString {
                append(summary)
                val start = summary.indexOf(amountText)
                if (start >= 0) addStyle(amountStyle, start, start + amountText.length)
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
