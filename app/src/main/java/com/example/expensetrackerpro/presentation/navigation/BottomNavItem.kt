import androidx.annotation.StringRes
import com.example.expensetrackerpro.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.HomeMax
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.material.icons.outlined.ReceiptLong

import androidx.compose.material.icons.rounded.ReceiptLong


sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        titleRes = R.string.nav_insights,
        selectedIcon = Icons.Rounded.HomeMax,
        unselectedIcon = Icons.Outlined.Home
    )
    object Transactions : BottomNavItem(
        route = "transactions",
        titleRes = R.string.nav_transactions,
        selectedIcon = Icons.Rounded.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong
    )
    object Settings : BottomNavItem(
        route = "settings",
        titleRes = R.string.nav_settings,
        selectedIcon = Icons.Rounded.SettingsSuggest,
        unselectedIcon = Icons.Outlined.Settings
    )
}