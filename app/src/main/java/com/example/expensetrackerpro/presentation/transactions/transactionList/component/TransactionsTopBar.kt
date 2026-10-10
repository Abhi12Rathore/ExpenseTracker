package com.example.expensetrackerpro.presentation.transactions.transactionList.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.example.expensetrackerpro.presentation.common.search.TopBarSearchField

/** The screen title with a search button, or, while searching, the search field with ✕ to leave it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsTopBar(
    isSearchOpen: Boolean,
    searchQuery: String,
    onOpenSearch: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            if (isSearchOpen) {
                TopBarSearchField(
                    query = searchQuery,
                    onQueryChange = onSearchChange,
                    placeholder = stringResource(R.string.transactions_search_placeholder)
                )
            } else {
                Text(
                    text = stringResource(R.string.transactions_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        actions = {
            if (isSearchOpen) {
                IconButton(onClick = onCloseSearch) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.transactions_search_close)
                    )
                }
            } else {
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.transactions_search)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
