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
import com.example.expensetrackerpro.presentation.homeScreen.component.DashboardHeader
import com.example.expensetrackerpro.presentation.homeScreen.component.DashboardSectionHeader
import com.example.expensetrackerpro.presentation.homeScreen.component.FinancialSummaryCard
import com.example.expensetrackerpro.presentation.homeScreen.component.HomeEmptyState
import com.example.expensetrackerpro.presentation.homeScreen.component.InsightSection
import com.example.expensetrackerpro.presentation.homeScreen.component.MetricGrid
import com.example.expensetrackerpro.presentation.homeScreen.component.RecentActivityTile
import com.example.expensetrackerpro.presentation.homeScreen.component.charts.CashFlowChart
import com.example.expensetrackerpro.presentation.homeScreen.component.charts.CategoryExpenseDonutChart
import com.example.expensetrackerpro.presentation.homeScreen.component.charts.MonthlyTrendBarChart
import com.example.expensetrackerpro.presentation.homeScreen.viewmodels.HomeViewModel


@Composable
fun HomeScreen(
    onViewAllTransactions: () -> Unit,
    onTransactionClick: (Int) -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    val summary by viewModel.financialSummary.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Greeting Layer
            item {
                DashboardHeader(userName = "Abhishek", currentMonth = "July 2026")
            }

            // 2. Hero Card Layer
            item {
                FinancialSummaryCard(data = summary)
            }

            // 3. Quick KPI Layer
            item {
                MetricGrid(data = summary)
            }

            item {
                CashFlowChart(uiState.dailyCashFlow)
            }

            // NEW: 5. Spending Breakdown (Donut)
            item {
                CategoryExpenseDonutChart(uiState.categoryExpense)
            }
            item {
                MonthlyTrendBarChart(uiState.monthlyTrend)
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
