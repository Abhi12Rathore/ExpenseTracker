package com.example.expensetrackerpro.presentation.homeScreen


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.expensetrackerpro.presentation.common.filter.components.PeriodFilter
import com.example.expensetrackerpro.presentation.common.filter.shortLabel
import com.example.expensetrackerpro.presentation.homeScreen.component.DashboardHeader
import com.example.expensetrackerpro.presentation.homeScreen.component.DashboardSectionHeader
import com.example.expensetrackerpro.presentation.homeScreen.component.FinancialSummaryCard
import com.example.expensetrackerpro.presentation.homeScreen.component.HomeEmptyState
import com.example.expensetrackerpro.presentation.homeScreen.component.InsightSection
import com.example.expensetrackerpro.presentation.homeScreen.component.RecentActivityTile
import com.example.expensetrackerpro.presentation.homeScreen.component.charts.CategorySpendingCard
import com.example.expensetrackerpro.presentation.homeScreen.component.charts.MonthlyTrendBarChart
import com.example.expensetrackerpro.presentation.homeScreen.component.charts.SpendingCard
import com.example.expensetrackerpro.domain.models.BucketSize
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.SpendingBucket
import com.example.expensetrackerpro.presentation.navigation.CategoryFilter
import com.example.expensetrackerpro.presentation.navigation.HistoryFilter
import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeEvent
import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeUiState
import com.example.expensetrackerpro.presentation.homeScreen.viewmodels.HomeViewModel
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.presentation.common.preview.PreviewData


@Composable
fun HomeScreen(
    onViewAllTransactions: () -> Unit,
    onTransactionClick: (Int) -> Unit,
    onOpenHistory: (HistoryFilter) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onViewAllTransactions = onViewAllTransactions,
        onTransactionClick = onTransactionClick,
        onOpenHistory = onOpenHistory
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onViewAllTransactions: () -> Unit,
    onTransactionClick: (Int) -> Unit,
    onOpenHistory: (HistoryFilter) -> Unit
) {
    val summary = uiState.financialSummary
    val recentTransactions = uiState.recentTransactions

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Greeting Layer
            item {
                DashboardHeader(
                    userName = "Abhishek",
                    dayPeriod = uiState.dayPeriod
                )
            }

            item {
                PeriodFilter(
                    state = uiState.filter,
                    onEvent = { onEvent(HomeEvent.Filter(it)) }
                )
            }

            // 2. Hero Card Layer
            item {
                FinancialSummaryCard(
                    data = summary,
                    periodLabel = uiState.filter.shortLabel(),
                    comparison = uiState.periodComparison,
                    balanceTrend = uiState.balanceTrend
                )
            }

            item {
                SpendingCard(
                    chart = uiState.spending,
                    isLoading = uiState.isLoading,
                    onBucketClick = { bucket ->
                        onOpenHistory(
                            HistoryFilter(bucket.toDateFilter(uiState.spending.bucketSize, uiState.filter.filter))
                        )
                    }
                )
            }

            item {
                CategorySpendingCard(
                    ranking = uiState.categoryRanking,
                    isLoading = uiState.isLoading,
                    onCategoryClick = { category ->
                        onOpenHistory(
                            HistoryFilter(
                                dateFilter = uiState.filter.filter,
                                category = CategoryFilter(category.categoryId, category.categoryName)
                            )
                        )
                    }
                )
            }

            // Comparing months only says something across a year
            if ((uiState.filter.filter as? DateFilter.Period)?.granularity == PeriodGranularity.YEAR) {
                item {
                    MonthlyTrendBarChart(uiState.monthlyTrend)
                }
            }

            item {
                InsightSection(
                    insights = uiState.insights
                )
            }

            item {
                DashboardSectionHeader(
                    title = "Recent Activity",
                    onAction = onViewAllTransactions
                )
            }
            if (recentTransactions.isEmpty()) {
                item {
                    HomeEmptyState()
                }
            }else{
            items(recentTransactions) { transaction ->
                RecentActivityTile(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction.id) }
                )
            }}

            // Padding at bottom for FAB/Nav
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

/**
 * What History is filtered to for a tapped bar. A month of a year view opens as that month, so
 * it can be stepped from there; anything else opens as exactly the days the bar covers.
 */
private fun SpendingBucket.toDateFilter(bucketSize: BucketSize, selected: DateFilter): DateFilter =
    if (bucketSize == BucketSize.MONTH && selected is DateFilter.Period) {
        DateFilter.Period(PeriodGranularity.MONTH, range.startMillis)
    } else {
        DateFilter.Custom(range)
    }

@Composable
fun InsightsPager() {
    val pagerState = rememberPagerState(pageCount = { 3 })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(end = 32.dp),
        pageSpacing = 16.dp
    ) { page ->
        val insight = when(page) {
            0 -> "Your spending on Food is 12% higher this week. Try to cook more at home! 🍳"
            1 -> "Great job! Your savings rate reached 35% this month. You're ahead of schedule. 📈"
            else -> "Subscription alert: You have 3 recurring payments due in the next 48 hours. 💳"
        }

        InsightCard(message = insight)
    }
}

@Composable
fun InsightCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HomeContentPreview() {
    ExpenseTrackerProTheme {
        HomeContent(
            uiState = HomeUiState(
                financialSummary = PreviewData.financialSummary,
                periodComparison = PreviewData.periodComparison,
                balanceTrend = PreviewData.balanceTrend,
                spending = PreviewData.dailySpending,
                categoryRanking = PreviewData.categoryRanking,
                monthlyTrend = PreviewData.monthlyTrend,
                recentTransactions = PreviewData.transactions,
                insights = PreviewData.insights
            ),
            onEvent = {},
            onViewAllTransactions = {},
            onTransactionClick = {},
            onOpenHistory = {}
        )
    }
}
