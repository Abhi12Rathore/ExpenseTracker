import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.expensetrackerpro.presentation.exportBackup.ExportBackupScreen
import com.example.expensetrackerpro.presentation.currency.CurrencyScreen
import com.example.expensetrackerpro.presentation.language.LanguageScreen
import com.example.expensetrackerpro.presentation.manageCategories.ManageCategoryScreen
import com.example.expensetrackerpro.presentation.navigation.Screens
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.AddEditTransactionScreen


@Composable
fun AppNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState
) {
    NavHost(
        navController = navController,
        startDestination = "main_shell"
    ) {
        // Destination 1: The Bottom Nav Shell
        composable("main_shell") {
            MainScreen(
                snackbarHostState = snackbarHostState,
                onNavigateToTransactionForm = { id ->
                    // Routes carry the plain number
                    navController.navigate(Screens.TransactionForm.getRoute(id?.value))
                },
                onNavigateToNewTransaction = { mode ->
                    navController.navigate(Screens.TransactionForm.newRoute(mode))
                },
                onNavigateToManageCategories = {
                    navController.navigate(Screens.ManageCategories.route)
                },
                onNavigateToExportBackup = {
                    navController.navigate(Screens.ExportBackup.route)
                },
                onNavigateToLanguage = {
                    navController.navigate(Screens.Language.route)
                },
                onNavigateToCurrency = {
                    navController.navigate(Screens.Currency.route)
                }
            )
        }

        // Destination 2: Full Screen Add/Edit Form
        composable(
            route = Screens.TransactionForm.route,
            arguments = listOf(
                navArgument(Screens.TransactionForm.ARG_TRANSACTION_ID) { type = NavType.IntType; defaultValue = -1 },
                navArgument(Screens.TransactionForm.ARG_MODE) { type = NavType.StringType; nullable = true }
            ),
            // The app shortcuts; the NavHost exists only once the app is unlocked, so they never open over the lock
            deepLinks = listOf(navDeepLink { uriPattern = Screens.TransactionForm.DEEP_LINK })
        ) {
            AddEditTransactionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Destination 3: Full Screen Manage Categories
        composable(Screens.ManageCategories.route) {
            ManageCategoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Destination 4: Full Screen Export & backup
        composable(Screens.ExportBackup.route) {
            ExportBackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Destination 5: Full Screen Language
        composable(Screens.Language.route) {
            LanguageScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Destination 6: Full Screen Currency
        composable(Screens.Currency.route) {
            CurrencyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}