package com.example.expensetrackerpro.presentation.manageCategories.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

/** Back and the screen title with a search button, or, while searching, the search field in the title's place. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesTopBar(
    isSearchOpen: Boolean,
    searchQuery: String,
    onNavigateBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.manage_categories_back)
                )
            }
        },
        title = {
            if (isSearchOpen) {
                TopBarSearchField(
                    query = searchQuery,
                    onQueryChange = onSearchChange,
                    placeholder = stringResource(R.string.manage_categories_search_placeholder)
                )
            } else {
                Text(
                    text = stringResource(R.string.manage_categories_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        actions = {
            if (isSearchOpen) {
                IconButton(onClick = onCloseSearch) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.manage_categories_search_close)
                    )
                }
            } else {
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.manage_categories_search)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
