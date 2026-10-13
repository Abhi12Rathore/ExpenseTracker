package com.example.expensetrackerpro.presentation.settings.uistate

import androidx.annotation.StringRes
import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.domain.models.CategoryCounts
import com.example.expensetrackerpro.domain.models.LanguageChange
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.models.UserProfile

data class SettingsUiState(
    val profile: UserProfile = UserProfile(),
    // The name as typed in the edit dialog; non-null while that dialog is showing
    val profileNameDraft: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: LanguageChoice = LanguageChoice.SystemDefault,
    // A language change that was just applied, until its Undo was taken or has gone
    val languageChange: LanguageChange? = null,
    val appLock: AppLockSettings = AppLockSettings(),
    val biometricStatus: BiometricStatus = BiometricStatus.AVAILABLE,
    val isLockTimeoutDialogOpen: Boolean = false,
    // True while the screen still has to open the prompt that confirms turning the lock on
    val isAuthRequested: Boolean = false,
    // Why turning the lock on failed, until the screen has shown it
    val appLockError: AuthResult? = null,
    val categoryCounts: CategoryCounts = CategoryCounts(),
    val versionName: String = "",
    // What has been typed to confirm deleting everything; non-null while that dialog is showing
    val deleteAllConfirmation: String? = null,
    // Debug builds only: the section that loads test data
    val isDeveloperSectionVisible: Boolean = false,
    val isLoadSampleDialogOpen: Boolean = false,
    // True while data is being loaded or deleted
    val isDataActionRunning: Boolean = false,
    // How the last data action ended, until the screen has shown it
    val message: SettingsMessage? = null
) {
    // Without a screen lock there is nothing to unlock with
    val isAppLockAvailable: Boolean get() = biometricStatus != BiometricStatus.NO_DEVICE_CREDENTIAL

    val isDeleteAllConfirmed: Boolean get() = deleteAllConfirmation?.trim() == DELETE_CONFIRMATION_WORD

    companion object {
        /** What has to be typed before everything is deleted. Not translated: it is typed, not read. */
        const val DELETE_CONFIRMATION_WORD = "DELETE"
    }
}

sealed interface SettingsMessage {
    data class SampleDataLoaded(val transactionCount: Int) : SettingsMessage
    data object AllDataDeleted : SettingsMessage
    data class Failed(@StringRes val messageRes: Int) : SettingsMessage
}
