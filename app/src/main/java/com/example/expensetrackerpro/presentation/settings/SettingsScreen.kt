package com.example.expensetrackerpro.presentation.settings

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.domain.models.CategoryCounts
import com.example.expensetrackerpro.domain.models.LanguageChange
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.models.UserProfile
import com.example.expensetrackerpro.presentation.common.filter.components.PillSegmentedControl
import com.example.expensetrackerpro.presentation.lock.rememberBiometricAuthenticator
import com.example.expensetrackerpro.presentation.settings.components.DeleteAllDataDialog
import com.example.expensetrackerpro.presentation.settings.components.EditProfileDialog
import com.example.expensetrackerpro.presentation.settings.components.LockTimeoutDialog
import com.example.expensetrackerpro.presentation.settings.components.ProfileCard
import com.example.expensetrackerpro.presentation.settings.components.SettingsDivider
import com.example.expensetrackerpro.presentation.settings.components.SettingsItem
import com.example.expensetrackerpro.presentation.settings.components.SettingsSectionCard
import com.example.expensetrackerpro.presentation.settings.components.SettingsSwitchItem
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsEvent
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsMessage
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsUiState
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.presentation.common.motion.LocalThemeTransition
import com.example.expensetrackerpro.presentation.common.motion.phoneDarkTheme
import com.example.expensetrackerpro.presentation.common.motion.rememberPressTracker
import com.example.expensetrackerpro.presentation.common.motion.trackPresses
import com.example.expensetrackerpro.ui.theme.extendedColors
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToExportBackup: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
        onNavigateToCategories = onNavigateToCategories,
        onNavigateToExportBackup = onNavigateToExportBackup,
        onNavigateToLanguage = onNavigateToLanguage
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToExportBackup: () -> Unit,
    onNavigateToLanguage: () -> Unit
) {
    SettingsDialogs(state = state, onEvent = onEvent)

    val snackbarHostState = remember { SnackbarHostState() }
    val appLockErrorText = state.appLockError?.enableErrorRes()?.let { stringResource(it) }
    LaunchedEffect(appLockErrorText) {
        if (appLockErrorText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(appLockErrorText)
        onEvent(SettingsEvent.AppLockErrorShown)
    }

    val messageText = when (val message = state.message) {
        is SettingsMessage.SampleDataLoaded -> pluralStringResource(
            R.plurals.settings_sample_data_loaded,
            message.transactionCount,
            message.transactionCount
        )
        SettingsMessage.AllDataDeleted -> stringResource(R.string.settings_all_data_deleted)
        is SettingsMessage.Failed -> stringResource(message.messageRes)
        null -> null
    }
    LaunchedEffect(state.message) {
        if (messageText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(messageText)
        onEvent(SettingsEvent.MessageShown)
    }

    LanguageChangedSnackbar(change = state.languageChange, snackbarHostState = snackbarHostState, onEvent = onEvent)

    val palette = MaterialTheme.extendedColors.categoryPalette

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )

            ProfileCard(
                profile = state.profile,
                onClick = { onEvent(SettingsEvent.ProfileClicked) }
            )

            SettingsSectionCard(title = stringResource(R.string.settings_section_appearance)) {
                val themeLabels = ThemeMode.entries.associateWith { stringResource(it.labelRes()) }
                val themeTransition = LocalThemeTransition.current
                val context = LocalContext.current
                val themePresses = rememberPressTracker()
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    iconTint = palette.amber,
                    title = stringResource(R.string.settings_theme),
                    subtitle = stringResource(R.string.settings_theme_subtitle),
                    below = {
                        PillSegmentedControl(
                            options = ThemeMode.entries,
                            selected = state.themeMode,
                            label = { themeLabels.getValue(it) },
                            onSelect = { mode ->
                                // Before the change is saved, so the root knows where to reveal it from
                                themeTransition?.onOptionTapped(
                                    from = state.themeMode,
                                    to = mode,
                                    phoneDark = context.phoneDarkTheme(),
                                    center = themePresses.take()
                                )
                                onEvent(SettingsEvent.ThemeModeSelected(mode))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .trackPresses(themePresses)
                        )
                    }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Language,
                    iconTint = palette.blue,
                    title = stringResource(R.string.settings_language),
                    // English in every language, so the row can be found in one that can't be read
                    subtitle = stringResource(R.string.settings_language_subtitle),
                    onClick = onNavigateToLanguage,
                    trailing = {
                        Text(
                            text = state.language.label(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            SettingsSectionCard(title = stringResource(R.string.settings_section_security)) {
                SettingsSwitchItem(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.settings_app_lock),
                    subtitle = stringResource(state.biometricStatus.appLockSubtitleRes()),
                    checked = state.appLock.enabled,
                    onCheckedChange = { onEvent(SettingsEvent.AppLockToggled(it)) },
                    iconTint = palette.teal,
                    // Stays switchable while on, so it can always be turned off
                    enabled = state.isAppLockAvailable || state.appLock.enabled
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Timer,
                    iconTint = palette.indigo,
                    title = stringResource(R.string.settings_lock_after),
                    subtitle = stringResource(state.appLock.timeout.labelRes()),
                    onClick = { onEvent(SettingsEvent.LockAfterClicked) },
                    enabled = state.appLock.enabled
                )
            }

            SettingsSectionCard(title = stringResource(R.string.settings_section_data)) {
                SettingsItem(
                    icon = Icons.Default.Category,
                    iconTint = palette.blueGrey,
                    title = stringResource(R.string.settings_categories),
                    subtitle = categoriesSubtitle(state.categoryCounts),
                    onClick = onNavigateToCategories
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.CloudUpload,
                    iconTint = palette.blue,
                    title = stringResource(R.string.settings_export),
                    subtitle = stringResource(R.string.settings_export_subtitle),
                    onClick = onNavigateToExportBackup
                )
            }

            SettingsSectionCard(title = stringResource(R.string.settings_section_about)) {
                SettingsItem(
                    icon = Icons.Default.Info,
                    iconTint = palette.purple,
                    title = stringResource(R.string.settings_version),
                    subtitle = state.versionName
                )
            }

            SettingsSectionCard(
                title = stringResource(R.string.settings_section_danger),
                titleColor = MaterialTheme.colorScheme.error
            ) {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = stringResource(R.string.settings_delete_all),
                    subtitle = stringResource(R.string.settings_delete_all_subtitle),
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { onEvent(SettingsEvent.DeleteAllDataClicked) },
                    enabled = !state.isDataActionRunning
                )
            }

            if (state.isDeveloperSectionVisible) {
                SettingsSectionCard(title = stringResource(R.string.settings_section_developer)) {
                    SettingsItem(
                        icon = Icons.Default.Science,
                        iconTint = palette.green,
                        title = stringResource(R.string.settings_load_sample_data),
                        subtitle = stringResource(R.string.settings_load_sample_data_subtitle),
                        onClick = { onEvent(SettingsEvent.LoadSampleDataClicked) },
                        enabled = !state.isDataActionRunning
                    )
                }
            }

            Text(
                text = stringResource(R.string.settings_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Says the language has changed, in the new language and in English, and offers to undo it for
 * [LANGUAGE_UNDO_MILLIS]. Shown only once the screen is drawn in the new language: until then
 * the message itself would still be in the old one.
 */
@Composable
private fun LanguageChangedSnackbar(
    change: LanguageChange?,
    snackbarHostState: SnackbarHostState,
    onEvent: (SettingsEvent) -> Unit
) {
    val shownLanguage = LocalConfiguration.current.locales[0].language
    val isInNewLanguage = change != null && shownLanguage == change.language.languageCode
    val changed = stringResource(R.string.language_changed)
    val changedInEnglish = stringResource(R.string.language_changed_english)
    val message = if (changed == changedInEnglish) {
        changed
    } else {
        stringResource(R.string.language_changed_both, changed, changedInEnglish)
    }
    val undoLabel = stringResource(R.string.language_undo)

    LaunchedEffect(change, isInNewLanguage) {
        if (!isInNewLanguage) return@LaunchedEffect
        // Longer than a snackbar's own durations, as the message may be in a language just learnt of
        val result = withTimeoutOrNull(LANGUAGE_UNDO_MILLIS) {
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Indefinite
            )
        }
        onEvent(
            if (result == SnackbarResult.ActionPerformed) {
                SettingsEvent.LanguageUndoClicked
            } else {
                SettingsEvent.LanguageChangeShown
            }
        )
    }
}

/** The language's own name for itself, which reads the same in every language. */
@Composable
private fun LanguageChoice.label(): String = when (this) {
    LanguageChoice.SystemDefault -> stringResource(R.string.language_system_default)
    is LanguageChoice.Specific -> language.nativeName
}

/** Whichever dialog the state says is open. */
@Composable
private fun SettingsDialogs(state: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
    state.profileNameDraft?.let { draft ->
        EditProfileDialog(
            name = draft,
            onNameChange = { onEvent(SettingsEvent.ProfileNameChanged(it)) },
            onSave = { onEvent(SettingsEvent.ProfileNameSaved) },
            onDismiss = { onEvent(SettingsEvent.ProfileDialogDismissed) }
        )
    }

    if (state.isLockTimeoutDialogOpen) {
        LockTimeoutDialog(
            selectedTimeout = state.appLock.timeout,
            onTimeoutSelected = { onEvent(SettingsEvent.LockTimeoutSelected(it)) },
            onDismiss = { onEvent(SettingsEvent.LockTimeoutDialogDismissed) }
        )
    }

    state.deleteAllConfirmation?.let { confirmation ->
        DeleteAllDataDialog(
            confirmation = confirmation,
            isConfirmEnabled = state.isDeleteAllConfirmed,
            onConfirmationChange = { onEvent(SettingsEvent.DeleteAllDataTextChanged(it)) },
            onConfirm = { onEvent(SettingsEvent.DeleteAllDataConfirmed) },
            onDismiss = { onEvent(SettingsEvent.DeleteAllDataDismissed) }
        )
    }

    if (state.isLoadSampleDialogOpen) {
        LoadSampleDataDialog(
            onConfirm = { onEvent(SettingsEvent.LoadSampleDataConfirmed) },
            onDismiss = { onEvent(SettingsEvent.LoadSampleDataDismissed) }
        )
    }
}

@Composable
private fun LoadSampleDataDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_load_sample_data_dialog_title)) },
        text = { Text(stringResource(R.string.settings_load_sample_data_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.settings_load_sample_data_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_dialog_cancel))
            }
        }
    )
}

/** "25 categories · 3 custom" */
@Composable
private fun categoriesSubtitle(counts: CategoryCounts): String = stringResource(
    R.string.settings_categories_subtitle,
    pluralStringResource(R.plurals.settings_categories_count, counts.total, counts.total),
    pluralStringResource(R.plurals.settings_categories_custom, counts.custom, counts.custom)
)

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

private const val LANGUAGE_UNDO_MILLIS = 8_000L

private val previewState = SettingsUiState(
    profile = UserProfile(name = "Abhishek"),
    categoryCounts = CategoryCounts(total = 28, custom = 3),
    versionName = "1.0"
)

@PreviewLightDark
@Composable
private fun SettingsContentPreview() {
    ExpenseTrackerProTheme {
        SettingsContent(
            state = previewState.copy(appLock = AppLockSettings(enabled = true)),
            onEvent = {},
            onNavigateToCategories = {},
            onNavigateToExportBackup = {},
            onNavigateToLanguage = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsContentDeveloperPreview() {
    ExpenseTrackerProTheme {
        SettingsContent(
            state = previewState.copy(profile = UserProfile(), isDeveloperSectionVisible = true),
            onEvent = {},
            onNavigateToCategories = {},
            onNavigateToExportBackup = {},
            onNavigateToLanguage = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsContentDeleteAllPreview() {
    ExpenseTrackerProTheme {
        SettingsContent(
            state = previewState.copy(deleteAllConfirmation = "DELETE"),
            onEvent = {},
            onNavigateToCategories = {},
            onNavigateToExportBackup = {},
            onNavigateToLanguage = {}
        )
    }
}
