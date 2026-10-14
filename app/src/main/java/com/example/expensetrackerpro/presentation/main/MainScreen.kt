import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.TransactionId
import com.example.expensetrackerpro.presentation.homeScreen.HomeScreen
import com.example.expensetrackerpro.presentation.navigation.Screens
import com.example.expensetrackerpro.presentation.settings.SettingsScreen
import com.example.expensetrackerpro.presentation.transactions.transactionList.TransactionListScreen

private val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Transactions,
    BottomNavItem.Settings
)

@Composable
fun MainScreen(
    snackbarHostState: SnackbarHostState,
    onNavigateToTransactionForm: (TransactionId?) -> Unit,
    onNavigateToManageCategories: () -> Unit,
    onNavigateToExportBackup: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToCurrency: () -> Unit
) {
    val bottomNavController = rememberNavController()

    Scaffold(
        // Shared by every tab, so a message outlives a switch between them
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
            // Transactions's route carries optional arguments, so compare what comes before them
            val currentRoute = navBackStackEntry?.destination?.route?.substringBefore('?')

            MainBottomBar(
                currentRoute = currentRoute,
                onTabSelected = { item ->
                    if (currentRoute != item.route) {
                        bottomNavController.navigate(item.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                },
                onAddClick = { onNavigateToTransactionForm(null) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                // The bar has taken the gesture bar inset, so the tabs must not add it again
                .consumeWindowInsets(innerPadding)
        ) {
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Home.route
            ) {
                composable(BottomNavItem.Home.route) {
                    HomeScreen(
                        onViewAllTransactions = {
                            // Logic to switch tab to Transactions
                            bottomNavController.navigate(BottomNavItem.Transactions.route) {
                                // Standard navigation behavior to avoid stack buildup
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onTransactionClick = { id ->
                            onNavigateToTransactionForm(id)
                        },
                        onOpenTransactions = { filter ->
                            // No restoreState: a chart opens Transactions on its own filter, not the last one
                            bottomNavController.navigate(Screens.Transactions.getRoute(filter)) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(
                    route = Screens.Transactions.route,
                    arguments = listOf(
                        navArgument(Screens.Transactions.ARG_START) {
                            type = NavType.LongType; defaultValue = Screens.Transactions.NO_VALUE
                        },
                        navArgument(Screens.Transactions.ARG_END) {
                            type = NavType.LongType; defaultValue = Screens.Transactions.NO_VALUE
                        },
                        navArgument(Screens.Transactions.ARG_GRANULARITY) {
                            type = NavType.StringType; nullable = true
                        },
                        navArgument(Screens.Transactions.ARG_CATEGORY_ID) {
                            type = NavType.LongType; defaultValue = Screens.Transactions.NO_VALUE
                        },
                        navArgument(Screens.Transactions.ARG_CATEGORY_NAME) {
                            type = NavType.StringType; nullable = true
                        }
                    )
                ) {
                    TransactionListScreen(
                        snackbarHostState = snackbarHostState,
                        onTransactionClick = { id -> onNavigateToTransactionForm(id) }
                    )
                }
                composable(BottomNavItem.Settings.route) {
                    SettingsScreen(
                        onNavigateToCategories = onNavigateToManageCategories,
                        onNavigateToExportBackup = onNavigateToExportBackup,
                        onNavigateToLanguage = onNavigateToLanguage,
                        onNavigateToCurrency = onNavigateToCurrency
                    )
                }
            }
        }
    }
}

@Composable
private fun MainBottomBar(
    currentRoute: String?,
    onTabSelected: (BottomNavItem) -> Unit,
    onAddClick: () -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        // The label names the tab
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.titleRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        // One line in every language: a longer name is cut rather than wrapped
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        // Last in the row, after the tabs, so it is in reach on every one of them
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier.padding(end = 16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.nav_add_transaction)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MainBottomBarHomePreview() {
    ExpenseTrackerProTheme {
        MainBottomBar(
            currentRoute = BottomNavItem.Home.route,
            onTabSelected = {},
            onAddClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun MainBottomBarHistoryPreview() {
    ExpenseTrackerProTheme {
        MainBottomBar(
            currentRoute = BottomNavItem.Transactions.route,
            onTabSelected = {},
            onAddClick = {}
        )
    }
}
