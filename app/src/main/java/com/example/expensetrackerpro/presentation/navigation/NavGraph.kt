import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.expensetrackerpro.presentation.exportBackup.ExportBackupScreen
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
                onNavigateToManageCategories = {
                    navController.navigate(Screens.ManageCategories.route)
                },
                onNavigateToExportBackup = {
                    navController.navigate(Screens.ExportBackup.route)
                }
            )
        }

        // Destination 2: Full Screen Add/Edit Form
        composable(
            route = Screens.TransactionForm.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.IntType; defaultValue = -1 })
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
    }
}