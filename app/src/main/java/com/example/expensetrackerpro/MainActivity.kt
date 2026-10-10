package com.example.expensetrackerpro

import AppNavHost
import android.app.UiModeManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.presentation.lock.LockScreen
import com.example.expensetrackerpro.presentation.main.MainViewModel
import com.example.expensetrackerpro.presentation.main.uistate.MainEffect
import com.example.expensetrackerpro.presentation.main.uistate.MainEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Follows the whole app rather than this activity, so rotation and dialogs never count as
    // leaving. Registering replays ON_START, which is harmless: nothing has been backgrounded yet.
    private val appVisibilityObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = viewModel.onEvent(MainEvent.AppForegrounded)

        override fun onStop(owner: LifecycleOwner) = viewModel.onEvent(MainEvent.AppBackgrounded)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the splash only while startup work is still running
        splashScreen.setKeepOnScreenCondition { !viewModel.uiState.value.isReady }
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.view.animate()
                .alpha(0f)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(250L)
                .withEndAction { splashScreenViewProvider.remove() }
                .start()
        }

        enableEdgeToEdge()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appVisibilityObserver)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val darkTheme = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(uiState.themeMode, uiState.isThemeLoaded) {
                if (uiState.isThemeLoaded) applyNightMode(uiState.themeMode)
            }

            LaunchedEffect(uiState.isAppLockEnabled, uiState.isLockStateKnown) {
                if (uiState.isLockStateKnown) applyRecentsPrivacy(uiState.isAppLockEnabled)
            }

            // Owned here rather than by a screen, so a message raised while a full screen form is
            // closing is still there when the tabs come back
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    // Launched separately so a snackbar on screen doesn't hold up the next effect
                    launch {
                        when (effect) {
                            is MainEffect.ShowUndoDelete -> {
                                // A newer delete replaces the snackbar of the one before
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = getString(R.string.transactions_transaction_deleted),
                                    actionLabel = getString(R.string.transactions_undo),
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.onEvent(MainEvent.UndoDelete(effect.transaction))
                                }
                            }
                            is MainEffect.ShowRestoreFailed -> snackbarHostState.showSnackbar(
                                getString(effect.messageRes)
                            )
                        }
                    }
                }
            }

            ExpenseTrackerProTheme(darkTheme = darkTheme) {
                // Both outlive the lock screen, so unlocking returns to the same screen and state
                val navController = rememberNavController()
                val appContentState = rememberSaveableStateHolder()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // The system bar insets are applied here once; consuming them stops the
                    // Scaffolds and bars nested inside from adding them again
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        if (uiState.isLocked) {
                            LockScreen(
                                isPromptRequested = uiState.isPromptRequested,
                                message = uiState.lockMessage,
                                onEvent = viewModel::onEvent
                            )
                        } else {
                            appContentState.SaveableStateProvider(APP_CONTENT_KEY) {
                                AppNavHost(
                                    navController = navController,
                                    snackbarHostState = snackbarHostState
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appVisibilityObserver)
        super.onDestroy()
    }

    /**
     * Keeps the app's content out of the Recents screen while the app lock is on.
     *
     * Android 13+ can hide just the Recents thumbnail. Older versions only offer FLAG_SECURE,
     * which also blocks screenshots, screen recording and casting of the app. That is the
     * accepted cost of not showing balances in Recents on those versions.
     */
    private fun applyRecentsPrivacy(hideContent: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(!hideContent)
        } else if (hideContent) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Android 12+ remembers this per app, so the next launch's system splash matches the theme
    private fun applyNightMode(themeMode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val nightMode = when (themeMode) {
            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
        }
        getSystemService(UiModeManager::class.java).setApplicationNightMode(nightMode)
    }

    private companion object {
        const val APP_CONTENT_KEY = "app_content"
    }
}
