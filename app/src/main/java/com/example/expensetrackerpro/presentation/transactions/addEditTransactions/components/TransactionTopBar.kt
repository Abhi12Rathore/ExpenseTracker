package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.TransactionType

/** Close when adding; back and delete when editing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionTopBar(
    isEditMode: Boolean,
    transactionType: TransactionType,
    onNavigateBack: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transactionType == TransactionType.INCOME
    val title = when {
        isEditMode && isIncome -> R.string.transaction_edit_income
        isEditMode -> R.string.transaction_edit_expense
        isIncome -> R.string.transaction_add_income
        else -> R.string.transaction_add_expense
    }

    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(title), fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                if (isEditMode) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.transaction_back)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.transaction_close)
                    )
                }
            }
        },
        actions = {
            if (isEditMode) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.transaction_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
