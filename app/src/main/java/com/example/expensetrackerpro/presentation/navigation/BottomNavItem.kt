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
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        title = "Insights",
        selectedIcon = Icons.Rounded.HomeMax,
        unselectedIcon = Icons.Outlined.Home
    )
    object Transactions : BottomNavItem(
        route = "transactions",
        title = "History",
        selectedIcon = Icons.Rounded.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong
    )
    object Settings : BottomNavItem(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Rounded.SettingsSuggest,
        unselectedIcon = Icons.Outlined.Settings
    )
}