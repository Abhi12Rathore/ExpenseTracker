import com.example.expensetrackerpro.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.expensetrackerpro.presentation.homeScreen.HomeScreen
import com.example.expensetrackerpro.presentation.navigation.Screens
import com.example.expensetrackerpro.presentation.settings.SettingsScreen
import com.example.expensetrackerpro.presentation.transactions.transactionList.TransactionListScreen
import androidx.compose.runtime.getValue // Add this
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight

// ... existing imports ...

@Composable
fun MainScreen(
    snackbarHostState: SnackbarHostState,
    onNavigateToTransactionForm: (Int?) -> Unit,
    onNavigateToManageCategories: () -> Unit
) {
    val bottomNavController = rememberNavController()

    Scaffold(
        // Shared by every tab, so a message outlives a switch between them
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.graphicsLayer {
                    shadowElevation = 25f
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    clip = true
                }
            ) {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                // History's route carries optional arguments, so compare what comes before them
                val currentRoute = navBackStackEntry?.destination?.route?.substringBefore('?')

                val items = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Transactions,
                    BottomNavItem.Settings
                )

                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val title = stringResource(item.titleRes)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            // IMPLEMENTED NAVIGATION LOGIC
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
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            unselectedIconColor = MaterialTheme.colorScheme.outline,
                            unselectedTextColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
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
                        onOpenHistory = { filter ->
                            // No restoreState: a chart opens History on its own filter, not the last one
                            bottomNavController.navigate(Screens.History.getRoute(filter)) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(
                    route = Screens.History.route,
                    arguments = listOf(
                        navArgument(Screens.History.ARG_START) {
                            type = NavType.LongType; defaultValue = Screens.History.NO_VALUE
                        },
                        navArgument(Screens.History.ARG_END) {
                            type = NavType.LongType; defaultValue = Screens.History.NO_VALUE
                        },
                        navArgument(Screens.History.ARG_GRANULARITY) {
                            type = NavType.StringType; nullable = true
                        },
                        navArgument(Screens.History.ARG_CATEGORY_ID) {
                            type = NavType.LongType; defaultValue = Screens.History.NO_VALUE
                        },
                        navArgument(Screens.History.ARG_CATEGORY_NAME) {
                            type = NavType.StringType; nullable = true
                        }
                    )
                ) {
                    TransactionListScreen(
                        snackbarHostState = snackbarHostState,
                        onAddClick = { onNavigateToTransactionForm(null) },
                        onTransactionClick = { id -> onNavigateToTransactionForm(id) }
                    )
                }
                composable(BottomNavItem.Settings.route) {
                    SettingsScreen(onNavigateToCategories = onNavigateToManageCategories)
                }
            }
        }
    }
}