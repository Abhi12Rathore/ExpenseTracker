package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.models.LockTimeout
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.AppLockRepository
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import com.example.expensetrackerpro.domain.repository.ThemePreferenceRepository
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import com.example.expensetrackerpro.domain.usecase.applock.GetAppLockSettingsUseCase
import com.example.expensetrackerpro.domain.usecase.applock.GetBiometricStatusUseCase
import com.example.expensetrackerpro.domain.usecase.applock.GetCurrentTimeMillisUseCase
import com.example.expensetrackerpro.domain.usecase.applock.ShouldLockUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.theme.GetThemeModeUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ObserveDeletedTransactionsUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.RestoreTransactionUseCase
import com.example.expensetrackerpro.fakes.FakeAppLockRepository
import com.example.expensetrackerpro.fakes.FakeBiometricAvailability
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.FakeThemePreferenceRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.presentation.main.MainViewModel
import com.example.expensetrackerpro.presentation.main.uistate.LockMessage
import com.example.expensetrackerpro.presentation.main.uistate.MainEffect
import com.example.expensetrackerpro.presentation.main.uistate.MainEvent
import com.example.expensetrackerpro.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val timeProvider = FakeTimeProvider(nowMillis = 1_000_000L)
    private val biometricAvailability = FakeBiometricAvailability()

    private fun mainViewModel(
        categoryRepository: CategoryRepository = FakeCategoryRepository(),
        themeRepository: ThemePreferenceRepository = FakeThemePreferenceRepository(),
        appLockRepository: AppLockRepository = FakeAppLockRepository(),
        transactionRepository: TransactionRepository = FakeTransactionRepository()
    ) = MainViewModel(
        GetCategoriesUseCase(categoryRepository),
        GetThemeModeUseCase(themeRepository),
        GetAppLockSettingsUseCase(appLockRepository),
        GetBiometricStatusUseCase(biometricAvailability),
        ShouldLockUseCase(timeProvider),
        GetCurrentTimeMillisUseCase(timeProvider),
        ObserveDeletedTransactionsUseCase(transactionRepository),
        RestoreTransactionUseCase(transactionRepository)
    )

    private fun lockedViewModel(timeout: LockTimeout = LockTimeout.SECONDS_30) =
        mainViewModel(appLockRepository = FakeAppLockRepository(AppLockSettings(true, timeout)))

    /** A view model with the lock on that has been unlocked, as after a normal launch. */
    private fun unlockedViewModel(timeout: LockTimeout = LockTimeout.SECONDS_30) =
        lockedViewModel(timeout).apply {
            onEvent(MainEvent.PromptLaunched)
            onEvent(MainEvent.AuthFinished(AuthResult.SUCCESS))
        }

    private fun MainViewModel.backgroundFor(millis: Long) {
        onEvent(MainEvent.AppBackgrounded)
        timeProvider.nowMillis += millis
        onEvent(MainEvent.AppForegrounded)
    }

    @Test
    fun exposesTheSavedThemeMode() = runTest {
        val viewModel = mainViewModel(themeRepository = FakeThemePreferenceRepository(ThemeMode.DARK))

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun themeModeFollowsLaterChanges() = runTest {
        val themeRepository = FakeThemePreferenceRepository(ThemeMode.LIGHT)
        val viewModel = mainViewModel(themeRepository = themeRepository)

        themeRepository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun notReadyUntilTheThemeModeIsLoaded() = runTest {
        // Categories are already there, so only the theme is holding the splash
        val themeRepository = PendingThemePreferenceRepository()
        val viewModel = mainViewModel(themeRepository = themeRepository)

        assertFalse(viewModel.uiState.value.isReady)

        themeRepository.emit(ThemeMode.DARK)

        assertTrue(viewModel.uiState.value.isReady)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun notReadyUntilCategoriesFirstLoad() = runTest {
        val repository = PendingCategoryRepository()
        val viewModel = mainViewModel(repository)

        assertFalse(viewModel.uiState.value.isReady)

        repository.emit(listOf(category(id = 1, name = "Food", type = TransactionType.EXPENSE)))

        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun readyOnceCategoriesAreLoaded() = runTest {
        val repository = FakeCategoryRepository(
            listOf(category(id = 1, name = "Food", type = TransactionType.EXPENSE))
        )

        val viewModel = mainViewModel(repository)

        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun readyEvenWhenThereAreNoCategories() = runTest {
        // Every category can be deleted, so an empty first load must not hold the splash
        val viewModel = mainViewModel(FakeCategoryRepository())

        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun readyEvenWhenTheFirstLoadNeverDeliversCategories() = runTest {
        // The load ends abnormally before any list arrives; the splash must still be released
        val repository = object : CategoryRepository by FakeCategoryRepository() {
            override fun getAllCategories(): Flow<List<Category>> =
                flow { throw CancellationException("load aborted") }
        }

        val viewModel = mainViewModel(repository)

        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun notReadyUntilTheAppLockSettingsAreLoaded() = runTest {
        val appLockRepository = PendingAppLockRepository()
        val viewModel = mainViewModel(appLockRepository = appLockRepository)

        assertFalse(viewModel.uiState.value.isReady)
        assertFalse(viewModel.uiState.value.isLockStateKnown)

        appLockRepository.emit(AppLockSettings(enabled = true))

        assertTrue(viewModel.uiState.value.isReady)
        assertTrue(viewModel.uiState.value.isLocked)
    }

    @Test
    fun lockOff_startsUnlocked() = runTest {
        val state = mainViewModel().uiState.value

        assertFalse(state.isLocked)
        assertFalse(state.isAppLockEnabled)
        assertFalse(state.isPromptRequested)
    }

    @Test
    fun lockOn_startsLockedAndAsksForThePrompt() = runTest {
        // Even the longest timeout locks on launch: a new process has no session to continue
        val state = lockedViewModel(LockTimeout.MINUTES_5).uiState.value

        assertTrue(state.isLocked)
        assertTrue(state.isAppLockEnabled)
        assertTrue(state.isPromptRequested)
        assertNull(state.lockMessage)
    }

    @Test
    fun lockOnButNoScreenLock_startsUnlocked() = runTest {
        biometricAvailability.current = BiometricStatus.NO_DEVICE_CREDENTIAL

        val state = lockedViewModel().uiState.value

        assertFalse(state.isLocked)
        assertTrue(state.isAppLockEnabled)
    }

    @Test
    fun promptLaunched_consumesTheRequest() = runTest {
        val viewModel = lockedViewModel()

        viewModel.onEvent(MainEvent.PromptLaunched)

        assertFalse(viewModel.uiState.value.isPromptRequested)
        assertTrue(viewModel.uiState.value.isLocked)
    }

    @Test
    fun authSucceeded_unlocks() = runTest {
        val viewModel = lockedViewModel()
        viewModel.onEvent(MainEvent.PromptLaunched)

        viewModel.onEvent(MainEvent.AuthFinished(AuthResult.SUCCESS))

        assertFalse(viewModel.uiState.value.isLocked)
        assertFalse(viewModel.uiState.value.isPromptRequested)
        assertTrue(viewModel.uiState.value.isAppLockEnabled)
    }

    @Test
    fun authFailures_stayLockedWithAMessage() = runTest {
        mapOf(
            AuthResult.CANCELED to LockMessage.CANCELED,
            AuthResult.LOCKOUT to LockMessage.LOCKOUT,
            AuthResult.LOCKOUT_PERMANENT to LockMessage.LOCKOUT_PERMANENT,
            AuthResult.ERROR to LockMessage.ERROR
        ).forEach { (result, message) ->
            val viewModel = lockedViewModel()
            viewModel.onEvent(MainEvent.PromptLaunched)

            viewModel.onEvent(MainEvent.AuthFinished(result))

            assertTrue(viewModel.uiState.value.isLocked)
            assertEquals(message, viewModel.uiState.value.lockMessage)
            assertFalse(viewModel.uiState.value.isPromptRequested)
        }
    }

    @Test
    fun screenLockRemovedWhileLocked_unlocksInsteadOfStrandingTheUser() = runTest {
        val viewModel = lockedViewModel()
        viewModel.onEvent(MainEvent.PromptLaunched)

        viewModel.onEvent(MainEvent.AuthFinished(AuthResult.NO_CREDENTIAL))

        assertFalse(viewModel.uiState.value.isLocked)
    }

    @Test
    fun unlockClicked_clearsTheMessageAndAsksForThePromptAgain() = runTest {
        val viewModel = lockedViewModel()
        viewModel.onEvent(MainEvent.PromptLaunched)
        viewModel.onEvent(MainEvent.AuthFinished(AuthResult.LOCKOUT))

        viewModel.onEvent(MainEvent.UnlockClicked)

        assertTrue(viewModel.uiState.value.isPromptRequested)
        assertNull(viewModel.uiState.value.lockMessage)
        assertTrue(viewModel.uiState.value.isLocked)
    }

    @Test
    fun unlockClickedWhileUnlocked_doesNothing() = runTest {
        val viewModel = unlockedViewModel()

        viewModel.onEvent(MainEvent.UnlockClicked)

        assertFalse(viewModel.uiState.value.isPromptRequested)
    }

    @Test
    fun backInsideTheTimeout_staysUnlocked() = runTest {
        val viewModel = unlockedViewModel(LockTimeout.SECONDS_30)

        viewModel.backgroundFor(29_999)

        assertFalse(viewModel.uiState.value.isLocked)
    }

    @Test
    fun backExactlyAtTheTimeout_locks() = runTest {
        val viewModel = unlockedViewModel(LockTimeout.SECONDS_30)

        viewModel.backgroundFor(30_000)

        assertTrue(viewModel.uiState.value.isLocked)
        assertTrue(viewModel.uiState.value.isPromptRequested)
    }

    @Test
    fun backAfterTheTimeout_locks() = runTest {
        val viewModel = unlockedViewModel(LockTimeout.MINUTE_1)

        viewModel.backgroundFor(60_001)

        assertTrue(viewModel.uiState.value.isLocked)
    }

    @Test
    fun immediately_locksOnEveryReturn() = runTest {
        val viewModel = unlockedViewModel(LockTimeout.IMMEDIATELY)

        viewModel.backgroundFor(0)

        assertTrue(viewModel.uiState.value.isLocked)
    }

    @Test
    fun foregroundedWithoutEverBackgrounding_doesNotLock() = runTest {
        // What a rotation looks like: the new activity's observer is told the app is in front
        val viewModel = unlockedViewModel(LockTimeout.IMMEDIATELY)

        viewModel.onEvent(MainEvent.AppForegrounded)

        assertFalse(viewModel.uiState.value.isLocked)
    }

    @Test
    fun eachReturnIsMeasuredFromItsOwnBackgrounding() = runTest {
        val viewModel = unlockedViewModel(LockTimeout.SECONDS_30)
        viewModel.backgroundFor(20_000)

        viewModel.backgroundFor(20_000)

        assertFalse(viewModel.uiState.value.isLocked)
    }

    @Test
    fun lockOff_neverLocksOnReturn() = runTest {
        val viewModel = mainViewModel()

        viewModel.backgroundFor(600_000)

        assertFalse(viewModel.uiState.value.isLocked)
    }

    @Test
    fun noScreenLock_doesNotLockOnReturn() = runTest {
        val viewModel = unlockedViewModel(LockTimeout.IMMEDIATELY)
        biometricAvailability.current = BiometricStatus.NO_DEVICE_CREDENTIAL

        viewModel.backgroundFor(1_000)

        assertFalse(viewModel.uiState.value.isLocked)
    }

    @Test
    fun unlockingThroughAScreenThatBackgroundsTheApp_doesNotLockAgain() = runTest {
        // Before Android 11 the screen lock check is a separate screen: the result arrives first,
        // and the app only then learns it is back in front
        val viewModel = lockedViewModel(LockTimeout.IMMEDIATELY)
        viewModel.onEvent(MainEvent.PromptLaunched)
        viewModel.onEvent(MainEvent.AppBackgrounded)
        timeProvider.nowMillis += 5_000

        viewModel.onEvent(MainEvent.AuthFinished(AuthResult.SUCCESS))
        viewModel.onEvent(MainEvent.AppForegrounded)

        assertFalse(viewModel.uiState.value.isLocked)
    }

    @Test
    fun returningWhileThePromptIsOpen_doesNotAskForAnotherPrompt() = runTest {
        val viewModel = lockedViewModel(LockTimeout.IMMEDIATELY)
        viewModel.onEvent(MainEvent.PromptLaunched)

        viewModel.backgroundFor(5_000)

        assertTrue(viewModel.uiState.value.isLocked)
        assertFalse(viewModel.uiState.value.isPromptRequested)
    }

    @Test
    fun returningToTheLockScreenAfterACancelledPrompt_asksForThePromptAgain() = runTest {
        val viewModel = lockedViewModel()
        viewModel.onEvent(MainEvent.PromptLaunched)
        viewModel.onEvent(MainEvent.AuthFinished(AuthResult.CANCELED))

        viewModel.backgroundFor(1_000)

        assertTrue(viewModel.uiState.value.isPromptRequested)
        assertNull(viewModel.uiState.value.lockMessage)
    }

    @Test
    fun turningTheLockOnMidSession_doesNotLockUntilTheNextTimeout() = runTest {
        val appLockRepository = FakeAppLockRepository()
        val viewModel = mainViewModel(appLockRepository = appLockRepository)

        appLockRepository.setSettings(AppLockSettings(enabled = true, timeout = LockTimeout.MINUTE_1))

        assertFalse(viewModel.uiState.value.isLocked)
        assertTrue(viewModel.uiState.value.isAppLockEnabled)

        viewModel.backgroundFor(60_000)

        assertTrue(viewModel.uiState.value.isLocked)
    }

    @Test
    fun aChangedTimeoutAppliesToTheNextReturn() = runTest {
        val appLockRepository = FakeAppLockRepository(AppLockSettings(true, LockTimeout.MINUTES_5))
        val viewModel = mainViewModel(appLockRepository = appLockRepository)
        viewModel.onEvent(MainEvent.PromptLaunched)
        viewModel.onEvent(MainEvent.AuthFinished(AuthResult.SUCCESS))

        appLockRepository.setSettings(AppLockSettings(true, LockTimeout.IMMEDIATELY))
        viewModel.backgroundFor(1)

        assertTrue(viewModel.uiState.value.isLocked)
    }

    @Test
    fun turningTheLockOff_unlocksAndStopsLocking() = runTest {
        val appLockRepository = FakeAppLockRepository(AppLockSettings(true, LockTimeout.IMMEDIATELY))
        val viewModel = mainViewModel(appLockRepository = appLockRepository)

        appLockRepository.setSettings(AppLockSettings(enabled = false))

        assertFalse(viewModel.uiState.value.isLocked)
        assertFalse(viewModel.uiState.value.isAppLockEnabled)
        assertFalse(viewModel.uiState.value.isPromptRequested)

        viewModel.backgroundFor(600_000)

        assertFalse(viewModel.uiState.value.isLocked)
    }

    @Test
    fun aLateResultWhileUnlocked_isIgnored() = runTest {
        val viewModel = unlockedViewModel()

        viewModel.onEvent(MainEvent.AuthFinished(AuthResult.ERROR))

        assertFalse(viewModel.uiState.value.isLocked)
        assertNull(viewModel.uiState.value.lockMessage)
    }

    // --- Undo delete

    private val lunch = transaction(id = 2, amount = rupees(250))
    private val rent = transaction(id = 3, amount = rupees(400))

    private fun TestScope.effectsOf(viewModel: MainViewModel): List<MainEffect> {
        val effects = mutableListOf<MainEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.toList(effects)
        }
        return effects
    }

    @Test
    fun deleteWithNoTransactionsScreenAlive_stillOffersUndo() = runTest {
        // As when Edit is opened from Home's recent activity: no Transactions view model exists,
        // and the only thing listening for deletions is the app itself
        val repository = FakeTransactionRepository(listOf(lunch, rent))
        val viewModel = mainViewModel(transactionRepository = repository)
        val effects = effectsOf(viewModel)

        DeleteTransactionUseCase(repository)(lunch)

        assertEquals(listOf<MainEffect>(MainEffect.ShowUndoDelete(lunch)), effects)
    }

    @Test
    fun undoOfferedBeforeAnyoneCollects_waitsForTheCollector() = runTest {
        // The delete happens on a full screen form, before the snackbar's collector is back
        val repository = FakeTransactionRepository(listOf(lunch, rent))
        val viewModel = mainViewModel(transactionRepository = repository)

        repository.deleteTransaction(lunch)

        assertEquals(listOf<MainEffect>(MainEffect.ShowUndoDelete(lunch)), effectsOf(viewModel))
    }

    @Test
    fun everyDeleteOffersItsOwnUndo() = runTest {
        val repository = FakeTransactionRepository(listOf(lunch, rent))
        val effects = effectsOf(mainViewModel(transactionRepository = repository))

        repository.deleteTransaction(lunch)
        repository.deleteTransaction(rent)

        assertEquals(
            listOf<MainEffect>(MainEffect.ShowUndoDelete(lunch), MainEffect.ShowUndoDelete(rent)),
            effects
        )
    }

    @Test
    fun failedDelete_offersNothing() = runTest {
        val repository = FakeTransactionRepository(listOf(lunch)).apply { failWrites = true }
        val effects = effectsOf(mainViewModel(transactionRepository = repository))

        repository.deleteTransaction(lunch)

        assertTrue(effects.isEmpty())
    }

    @Test
    fun undoDelete_restoresTheTransaction() = runTest {
        val repository = FakeTransactionRepository(listOf(lunch, rent))
        val viewModel = mainViewModel(transactionRepository = repository)
        val effects = effectsOf(viewModel)
        repository.deleteTransaction(lunch)

        viewModel.onEvent(MainEvent.UndoDelete(lunch))

        assertEquals(listOf(lunch, rent), repository.current.sortedBy { it.id })
        assertEquals(listOf<MainEffect>(MainEffect.ShowUndoDelete(lunch)), effects)
    }

    @Test
    fun failedUndo_isReported() = runTest {
        val repository = FakeTransactionRepository(listOf(lunch, rent))
        val viewModel = mainViewModel(transactionRepository = repository)
        val effects = effectsOf(viewModel)
        repository.deleteTransaction(lunch)
        repository.failWrites = true

        viewModel.onEvent(MainEvent.UndoDelete(lunch))

        assertEquals(MainEffect.ShowRestoreFailed(R.string.error_storage_failure), effects.last())
        assertEquals(listOf(rent), repository.current)
    }

    @Test
    fun anUndoWhoseCategoryWasDeletedMeanwhile_saysTheCategoryNoLongerExists() = runTest {
        val repository = FakeTransactionRepository(listOf(lunch, rent))
        val viewModel = mainViewModel(transactionRepository = repository)
        val effects = effectsOf(viewModel)
        repository.deleteTransaction(lunch)
        repository.failWrites = true
        repository.writeFailure = DomainError.NotFound

        viewModel.onEvent(MainEvent.UndoDelete(lunch))

        assertEquals(MainEffect.ShowRestoreFailed(R.string.error_category_not_found), effects.last())
    }
}

/** Emits nothing until [emit] is called, like preferences that are still being read from disk. */
private class PendingAppLockRepository : AppLockRepository {

    private val settings = MutableSharedFlow<AppLockSettings>()

    suspend fun emit(value: AppLockSettings) = settings.emit(value)

    override fun getSettings(): Flow<AppLockSettings> = settings

    override suspend fun setSettings(settings: AppLockSettings) = Unit
}

/** Emits nothing until [emit] is called, like a database that is still opening. */
private class PendingCategoryRepository : CategoryRepository {

    private val categories = MutableSharedFlow<List<Category>>()

    suspend fun emit(list: List<Category>) = categories.emit(list)

    override fun getAllCategories(): Flow<List<Category>> = categories

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        categories.map { list -> list.filter { it.type == type } }

    override suspend fun insertCategory(category: Category): Int = category.id

    override suspend fun updateCategory(category: Category) = Unit

    override suspend fun deleteCategory(category: Category): Outcome<Unit> = Outcome.Success(Unit)

    override fun getCategoryUsage(range: DateRange): Flow<List<CategoryUsage>> =
        categories.map { list -> list.map { CategoryUsage(it) } }

    override suspend fun deleteCategoryReassigning(category: Category, target: Category): Outcome<Unit> =
        Outcome.Success(Unit)
}

/** Emits nothing until [emit] is called, like preferences that are still being read from disk. */
private class PendingThemePreferenceRepository : ThemePreferenceRepository {

    private val themeMode = MutableSharedFlow<ThemeMode>()

    suspend fun emit(mode: ThemeMode) = themeMode.emit(mode)

    override fun getThemeMode(): Flow<ThemeMode> = themeMode

    override suspend fun setThemeMode(mode: ThemeMode) = Unit
}
