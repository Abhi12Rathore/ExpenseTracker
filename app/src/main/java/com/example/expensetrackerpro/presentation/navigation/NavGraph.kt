import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.expensetrackerpro.presentation.manageCategories.ManageCategoryScreen
import com.example.expensetrackerpro.presentation.navigation.Screens
import com.example.expensetrackerpro.presentation.splash.SplashScreen
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.AddEditTransactionScreen


@Composable
fun AppNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "splash" // Updated Start Destination
    ) {
        // Destination 0: Premium Splash Screen
        composable("splash") {
            SplashScreen(
                onTimeout = {
                    navController.navigate("main_shell") {
                        // Pop up to "splash" and include it to clear backstack
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // Destination 1: The Bottom Nav Shell
        composable("main_shell") {
            MainScreen(
                onNavigateToTransactionForm = { id ->
                    navController.navigate(Screens.TransactionForm.getRoute(id ?: -1))
                },
                onNavigateToManageCategories = {
                    navController.navigate(Screens.ManageCategories.route)
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
    }
}