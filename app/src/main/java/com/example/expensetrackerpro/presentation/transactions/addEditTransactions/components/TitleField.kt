package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.TransactionType

@Composable
fun TitleField(
    title: String,
    transactionType: TransactionType,
    isError: Boolean,
    onTitleChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isFromBill: Boolean = false
) {
    val placeholder =
        if (transactionType == TransactionType.INCOME) R.string.transaction_title_placeholder_income
        else R.string.transaction_title_placeholder_expense

    Column(modifier = modifier.fillMaxWidth()) {
        if (isFromBill) FromBillTag()

        TitleTextField(title, placeholder, isError, onTitleChange, onFocusChange, isFromBill)
    }
}

@Composable
private fun TitleTextField(
    title: String,
    @StringRes placeholder: Int,
    isError: Boolean,
    onTitleChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    isFromBill: Boolean
) {
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChange(it.isFocused) },
        label = { Text(stringResource(R.string.transaction_title)) },
        placeholder = { Text(stringResource(placeholder)) },
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.transaction_title_required)) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        ),
        shape = RoundedCornerShape(12.dp),
        colors = billFieldColors(isFromBill)
    )
}
