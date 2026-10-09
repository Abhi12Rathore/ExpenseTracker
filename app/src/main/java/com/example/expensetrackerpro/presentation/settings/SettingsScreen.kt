package com.example.expensetrackerpro.presentation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.compose.ui.draw.alpha
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.presentation.lock.rememberBiometricAuthenticator
import com.example.expensetrackerpro.presentation.settings.components.LockTimeoutDialog
import com.example.expensetrackerpro.presentation.settings.components.SettingsSwitchItem
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.presentation.settings.components.ThemeDialog
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsEvent
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsUiState
import com.example.expensetrackerpro.ui.theme.extendedColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.example.expensetrackerpro.ExpenseTrackerProTheme

@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val authenticator = rememberBiometricAuthenticator {
        viewModel.onEvent(SettingsEvent.AppLockAuthFinished(it))
    }
    val promptTitle = stringResource(R.string.app_lock_enable_prompt_title)
    val promptSubtitle = stringResource(R.string.app_lock_enable_prompt_subtitle)

    LaunchedEffect(uiState.isAuthRequested, authenticator) {
        if (!uiState.isAuthRequested || authenticator == null) return@LaunchedEffect
        viewModel.onEvent(SettingsEvent.AppLockPromptLaunched)
        authenticator.authenticate(promptTitle, promptSubtitle)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(SettingsEvent.ScreenResumed)
        onPauseOrDispose {}
    }

    SettingsContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToCategories = onNavigateToCategories
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onNavigateToCategories: () -> Unit
) {
    if (state.isThemeDialogOpen) {
        ThemeDialog(
            selectedMode = state.themeMode,
            onModeSelected = { onEvent(SettingsEvent.ThemeModeSelected(it)) },
            onDismiss = { onEvent(SettingsEvent.ThemeDialogDismissed) }
        )
    }

    if (state.isLockTimeoutDialogOpen) {
        LockTimeoutDialog(
            selectedTimeout = state.appLock.timeout,
            onTimeoutSelected = { onEvent(SettingsEvent.LockTimeoutSelected(it)) },
            onDismiss = { onEvent(SettingsEvent.LockTimeoutDialogDismissed) }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val appLockErrorText = state.appLockError?.enableErrorRes()?.let { stringResource(it) }
    LaunchedEffect(appLockErrorText) {
        if (appLockErrorText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(appLockErrorText)
        onEvent(SettingsEvent.AppLockErrorShown)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = "Manage your application settings",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier =Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            SettingsSection(title = "Appearance") {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.settings_theme),
                    subtitle = stringResource(state.themeMode.labelRes()),
                    iconTint = MaterialTheme.extendedColors.categoryPalette.amber,
                    onClick = { onEvent(SettingsEvent.ThemeClicked) }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_security)) {
                SettingsSwitchItem(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.settings_app_lock),
                    subtitle = stringResource(state.biometricStatus.appLockSubtitleRes()),
                    checked = state.appLock.enabled,
                    onCheckedChange = { onEvent(SettingsEvent.AppLockToggled(it)) },
                    iconTint = MaterialTheme.extendedColors.categoryPalette.teal,
                    // Stays switchable while on, so it can always be turned off
                    enabled = state.isAppLockAvailable || state.appLock.enabled
                )
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.settings_lock_after),
                    subtitle = stringResource(state.appLock.timeout.labelRes()),
                    onClick = { onEvent(SettingsEvent.LockAfterClicked) },
                    iconTint = MaterialTheme.extendedColors.categoryPalette.indigo,
                    enabled = state.appLock.enabled
                )
            }

            SettingsSection(title = "Categories") {
                SettingsItem(
                    icon = Icons.Default.Label,
                    title = "Manage Categories",
                    subtitle = "Add, edit or delete categories",
                    onClick = onNavigateToCategories,
                    iconTint = MaterialTheme.extendedColors.categoryPalette.blueGrey,
                )
            }

            SettingsSection(title = "Data") {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Reset All Data",
                    subtitle = "Permanently delete all transactions",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { /* Show Confirmation Dialog */ },
                    iconTint = MaterialTheme.colorScheme.error,
                )
            }

            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.0 (Stable)",
                    onClick = {},
                    iconTint = MaterialTheme.extendedColors.categoryPalette.purple,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "❤️ Made with Jetpack Compose",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        content()
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    iconTint:Color,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .alpha(if (enabled) 1f else DISABLED_ALPHA),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = titleColor, fontWeight = FontWeight.Medium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// Material's opacity for disabled content
private const val DISABLED_ALPHA = 0.38f

@StringRes
private fun BiometricStatus.appLockSubtitleRes(): Int = when (this) {
    BiometricStatus.AVAILABLE -> R.string.settings_app_lock_biometric
    BiometricStatus.NO_HARDWARE,
    BiometricStatus.NONE_ENROLLED -> R.string.settings_app_lock_credential
    BiometricStatus.NO_DEVICE_CREDENTIAL -> R.string.settings_app_lock_unavailable
}

// Success and cancel need no explanation
@StringRes
private fun AuthResult.enableErrorRes(): Int? = when (this) {
    AuthResult.SUCCESS, AuthResult.CANCELED -> null
    AuthResult.LOCKOUT -> R.string.app_lock_enable_lockout
    AuthResult.LOCKOUT_PERMANENT -> R.string.app_lock_enable_lockout_permanent
    AuthResult.NO_CREDENTIAL -> R.string.app_lock_enable_no_credential
    AuthResult.ERROR -> R.string.app_lock_enable_error
}

@PreviewLightDark
@Composable
private fun LockAfterRowPreview() {
    ExpenseTrackerProTheme {
        Surface {
            Column {
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.settings_lock_after),
                    subtitle = stringResource(R.string.lock_timeout_30_seconds),
                    onClick = {},
                    iconTint = MaterialTheme.extendedColors.categoryPalette.indigo
                )
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.settings_lock_after),
                    subtitle = stringResource(R.string.lock_timeout_30_seconds),
                    onClick = {},
                    iconTint = MaterialTheme.extendedColors.categoryPalette.indigo,
                    enabled = false
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SettingsContentAppLockOnPreview() {
    ExpenseTrackerProTheme {
        SettingsContent(
            state = SettingsUiState(appLock = AppLockSettings(enabled = true)),
            onEvent = {},
            onNavigateToCategories = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsContentPreview() {
    ExpenseTrackerProTheme {
        SettingsContent(
            state = SettingsUiState(),
            onEvent = {},
            onNavigateToCategories = {}
        )
    }
}
