package com.example.expensetrackerpro.presentation.currency

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.CurrencyPreview
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.presentation.common.haptics.rememberHaptics
import com.example.expensetrackerpro.presentation.common.list.GroupedRow
import com.example.expensetrackerpro.presentation.common.list.SectionTitle
import com.example.expensetrackerpro.presentation.common.motion.LocalAnimationsEnabled
import com.example.expensetrackerpro.presentation.currency.components.CurrencyConfirmDialog
import com.example.expensetrackerpro.presentation.currency.components.CurrencyPreviewCard
import com.example.expensetrackerpro.presentation.currency.components.CurrencyRow
import com.example.expensetrackerpro.presentation.currency.uistate.CurrencyEvent
import com.example.expensetrackerpro.presentation.currency.uistate.CurrencyUiState
import com.example.expensetrackerpro.utils.nameRes

private val SECTION_GAP = 20.dp

// Below this the preview shows only the balance and its row, leaving room for the list
private const val COMPACT_SCREEN_HEIGHT_DP = 640

// Stable keys, so a selection never makes the list re-lay out its rows
private const val KEY_SEARCH = "search"
private const val KEY_HEADER_SUGGESTED = "header_suggested"
private const val KEY_HEADER_ALL = "header_all"
private const val KEY_NO_MATCH = "no_match"

// The suggested rows are keyed apart from the full list's, so a currency can never share a key
private const val KEY_SUGGESTED_PREFIX = "suggested_"

@Composable
fun CurrencyScreen(
    onNavigateBack: () -> Unit,
    viewModel: CurrencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isApplied) {
        // Settings offers the Undo
        if (uiState.isApplied) onNavigateBack()
    }

    CurrencyContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyContent(
    state: CurrencyUiState,
    onEvent: (CurrencyEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    state.confirmation?.let { example ->
        CurrencyConfirmDialog(
            example = example,
            from = state.current,
            to = state.selected,
            onConfirm = { onEvent(CurrencyEvent.ConfirmClicked) },
            onDismiss = { onEvent(CurrencyEvent.ConfirmDismissed) }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.currency_back)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.currency_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        // Only the top: the list runs on under the gesture bar and the apply bar, and pads its own end
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            // Saved, so a list restored near its end has its full padding before the bar is measured again
            var applyBarHeight by rememberSaveable { mutableIntStateOf(0) }
            val density = LocalDensity.current
            val animationsEnabled = LocalAnimationsEnabled.current
            val listBottomPadding by animateDpAsState(
                targetValue = if (state.isApplyVisible) with(density) { applyBarHeight.toDp() } else 0.dp,
                animationSpec = if (animationsEnabled) spring() else snap(),
                label = "listBottomPadding"
            )

            Column(modifier = Modifier.fillMaxSize()) {
                PinnedPreview(state = state)
                CurrencyList(
                    state = state,
                    onEvent = onEvent,
                    bottomPadding = listBottomPadding,
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(
                visible = state.isApplyVisible,
                enter = if (animationsEnabled) slideInVertically { it } + fadeIn() else EnterTransition.None,
                exit = if (animationsEnabled) slideOutVertically { it } + fadeOut() else ExitTransition.None,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.onSizeChanged { applyBarHeight = it.height }
                ) {
                    ApplyCurrencyButton(
                        currency = state.selected,
                        onClick = { onEvent(CurrencyEvent.ApplyClicked) }
                    )
                }
            }
        }
    }
}

/**
 * The user's figures in the selected currency, above the list and outside it, so picking a
 * currency changes only what the card says: the list beneath never moves.
 */
@Composable
private fun PinnedPreview(state: CurrencyUiState) {
    val compact = LocalConfiguration.current.screenHeightDp < COMPACT_SCREEN_HEIGHT_DP
    // Until the figures have loaded the card is laid out empty, so it has its size from the start
    val preview = state.preview ?: CurrencyPreview(summary = FinancialSummary(), topCategory = null)
    // The tallest it has been: a currency written wider can't push the list down and up
    var tallest by remember(compact) { mutableIntStateOf(0) }
    val density = LocalDensity.current

    CurrencyPreviewCard(
        preview = preview,
        currency = state.selected,
        compact = compact,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
            .heightIn(min = with(density) { tallest.toDp() })
            .onSizeChanged { tallest = maxOf(tallest, it.height) }
            .alpha(if (state.preview == null) 0f else 1f)
    )
}

@Composable
private fun CurrencyList(
    state: CurrencyUiState,
    onEvent: (CurrencyEvent) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Saveable: the scroll position survives rotation and the process being stopped
    val listState = rememberLazyListState()
    val suggested = state.visibleSuggested
    val others = state.visibleOthers

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp + maxOf(bottomPadding, navigationBar)
        )
    ) {
        item(key = KEY_SEARCH) {
            SearchField(
                query = state.query,
                onQueryChange = { onEvent(CurrencyEvent.QueryChanged(it)) },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (suggested.isNotEmpty()) {
            item(key = KEY_HEADER_SUGGESTED) {
                SectionTitle(stringResource(R.string.currency_section_suggested), Modifier.padding(bottom = 8.dp))
            }
            itemsIndexed(suggested, key = { _, currency -> KEY_SUGGESTED_PREFIX + currency.code }) { index, currency ->
                GroupedRow(index = index, count = suggested.size) {
                    CurrencyChoiceRow(currency = currency, state = state, onEvent = onEvent)
                }
            }
        }

        if (others.isNotEmpty()) {
            item(key = KEY_HEADER_ALL) {
                SectionTitle(
                    stringResource(R.string.currency_section_all),
                    Modifier.padding(top = if (suggested.isEmpty()) 0.dp else SECTION_GAP, bottom = 8.dp)
                )
            }
            itemsIndexed(others, key = { _, currency -> currency.code }) { index, currency ->
                GroupedRow(index = index, count = others.size) {
                    CurrencyChoiceRow(currency = currency, state = state, onEvent = onEvent)
                }
            }
        }

        if (suggested.isEmpty() && others.isEmpty()) {
            item(key = KEY_NO_MATCH) {
                Text(
                    text = stringResource(R.string.currency_search_no_match, state.query.trim()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun CurrencyChoiceRow(
    currency: AppCurrency,
    state: CurrencyUiState,
    onEvent: (CurrencyEvent) -> Unit
) {
    CurrencyRow(
        currency = currency,
        isCurrent = currency == state.current,
        selected = currency == state.selected,
        onClick = { onEvent(CurrencyEvent.CurrencySelected(currency)) }
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.currency_search_hint),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.currency_search_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun ApplyCurrencyButton(currency: AppCurrency, onClick: () -> Unit) {
    val haptics = rememberHaptics()

    Button(
        onClick = {
            haptics.confirm()
            onClick()
        },
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .heightIn(min = 56.dp)
    ) {
        Text(
            text = stringResource(R.string.currency_use, stringResource(currency.nameRes())),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val previewState = CurrencyUiState(
    current = AppCurrency.INR,
    selected = AppCurrency.USD,
    suggested = listOf(AppCurrency.INR, AppCurrency.USD),
    others = AppCurrency.entries - setOf(AppCurrency.INR, AppCurrency.USD),
    preview = CurrencyPreview(
        summary = FinancialSummary(
            totalBalance = Money.ofRupees(50_422),
            totalIncome = Money.ofRupees(105_500),
            totalExpense = Money.ofRupees(55_078),
            savingsRate = 47f
        ),
        topCategory = null
    )
)

@PreviewLightDark
@Composable
private fun CurrencyContentPreview() {
    ExpenseTrackerProTheme {
        CurrencyContent(state = previewState, onEvent = {}, onNavigateBack = {})
    }
}
