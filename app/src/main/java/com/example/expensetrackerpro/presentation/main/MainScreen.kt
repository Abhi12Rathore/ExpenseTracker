import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEntryMode
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
    onNavigateToNewTransaction: (AddEntryMode) -> Unit,
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
                onAddClick = { onNavigateToNewTransaction(AddEntryMode.MANUAL) },
                onAddModeSelected = onNavigateToNewTransaction
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
    onAddClick: () -> Unit,
    onAddModeSelected: (AddEntryMode) -> Unit
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
        AddButton(
            onClick = onAddClick,
            onModeSelected = onAddModeSelected,
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}

private val AddButtonSize = 56.dp

/** The ways to start a transaction offered on a long press, in the order the menu lists them. */
private val addModes = listOf(
    Triple(AddEntryMode.QUICK_ADD, Icons.Rounded.AutoAwesome, R.string.add_mode_quick_add),
    Triple(AddEntryMode.SCAN, Icons.Rounded.PhotoCamera, R.string.add_mode_scan),
    Triple(AddEntryMode.VOICE, Icons.Rounded.Mic, R.string.add_mode_voice),
    Triple(AddEntryMode.MANUAL, Icons.Rounded.Edit, R.string.add_mode_manual)
)

/**
 * The + button: a tap opens the form as always, a long press a short menu of the other ways to
 * start one (quick add, a bill, the voice). Drawn like a FloatingActionButton, which has no long press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddButton(
    onClick: () -> Unit,
    onModeSelected: (AddEntryMode) -> Unit,
    modifier: Modifier = Modifier
) {
    // Purely visual: whether the long-press menu is open
    var isMenuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.size(AddButtonSize),
            shape = FloatingActionButtonDefaults.shape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier.combinedClickable(
                    role = Role.Button,
                    onLongClickLabel = stringResource(R.string.add_mode_menu_label),
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        isMenuOpen = true
                    },
                    onClick = onClick
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.nav_add_transaction)
                )
            }
        }
        DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
            addModes.forEach { (mode, icon, labelRes) ->
                AddModeItem(icon = icon, label = stringResource(labelRes)) {
                    isMenuOpen = false
                    onModeSelected(mode)
                }
            }
        }
    }
}

@Composable
private fun AddModeItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick
    )
}

@PreviewLightDark
@Composable
private fun MainBottomBarHomePreview() {
    ExpenseTrackerProTheme {
        MainBottomBar(
            currentRoute = BottomNavItem.Home.route,
            onTabSelected = {},
            onAddClick = {},
            onAddModeSelected = {}
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
            onAddClick = {},
            onAddModeSelected = {}
        )
    }
}
