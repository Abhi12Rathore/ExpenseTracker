package com.example.expensetrackerpro.presentation

import java.util.Locale
import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import com.example.expensetrackerpro.fakes.HINDI
import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.fakes.localizeDefaultCategories
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryId
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
import com.example.expensetrackerpro.presentation.lock.AppLockController
import com.example.expensetrackerpro.presentation.lock.LockMessage
import com.example.expensetrackerpro.presentation.main.MainViewModel
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
        transactionRepository: TransactionRepository = FakeTransactionRepository(),
        localeProvider: AppLocaleProvider = FakeAppLocaleProvider(),
        // The activity reports itself created as soon as the app starts
        isActivityCreated: Boolean = true
    ) = MainViewModel(
        localizeDefaultCategories(categoryRepository, localeProvider),
        GetThemeModeUseCase(themeRepository),
        ObserveDeletedTransactionsUseCase(transactionRepository),
        RestoreTransactionUseCase(transactionRepository),
        AppLockController(
            GetAppLockSettingsUseCase(appLockRepository),
            GetBiometricStatusUseCase(biometricAvailability),
            ShouldLockUseCase(timeProvider),
            GetCurrentTimeMillisUseCase(timeProvider)
        )
    ).apply { if (isActivityCreated) onEvent(MainEvent.ActivityCreated) }

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
    fun notReadyUntilTheActivityIsCreated() = runTest {
        val viewModel = mainViewModel(isActivityCreated = false)

        assertFalse(viewModel.uiState.value.isReady)

        viewModel.onEvent(MainEvent.ActivityCreated)

        assertTrue(viewModel.uiState.value.isReady)
    }

    @Test
    fun builtInCategories_takeTheAppsLanguage_atStartAndWheneverTheActivityIsCreatedAgain() = runTest {
        val repository = FakeCategoryRepository(
            listOf(
                category(id = 1, name = "Fuel").copy(isCustom = false, builtInKey = BuiltInCategory.FUEL),
                category(id = 2, name = "Pets")
            )
        )
        val localeProvider = FakeAppLocaleProvider(HINDI)
        val viewModel = mainViewModel(repository, localeProvider = localeProvider)

        assertEquals(listOf("ईंधन", "Pets"), repository.current.map { it.name })

        // A change of language creates the activity again
        localeProvider.locale = Locale.ENGLISH
        viewModel.onEvent(MainEvent.ActivityCreated)

        assertEquals(listOf("Fuel", "Pets"), repository.current.map { it.name })
    }

    @Test
    fun readyEvenWhenTheCategoriesCouldNotBeRenamed() = runTest {
        val repository = FakeCategoryRepository(
            listOf(category(id = 1, name = "Fuel").copy(isCustom = false, builtInKey = BuiltInCategory.FUEL))
        ).apply { saveFailure = DomainError.StorageFailure() }

        val viewModel = mainViewModel(repository, localeProvider = FakeAppLocaleProvider(HINDI))

        assertTrue(viewModel.uiState.value.isReady)
        assertEquals("Fuel", repository.current.single().name)
    }

    @Test
    fun aChangeOfLanguage_whichCreatesTheActivityAgain_doesNotLockTheApp() = runTest {
        val viewModel = unlockedViewModel(LockTimeout.IMMEDIATELY)

        // What the activity reports when it is created again: it was never in the background
        viewModel.onEvent(MainEvent.ActivityCreated)
        viewModel.onEvent(MainEvent.AppForegrounded)

        assertFalse(viewModel.uiState.value.isLocked)
        assertFalse(viewModel.uiState.value.isPromptRequested)
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

    // --- App lock: the logic is AppLockController's, tested there

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
    fun theLockState_isShownAsTheControllerReportsIt() = runTest {
        val viewModel = lockedViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.isLockStateKnown)
        assertTrue(state.isAppLockEnabled)
        assertTrue(state.isLocked)
        assertTrue(state.isPromptRequested)
        assertNull(state.lockMessage)
    }

    @Test
    fun promptLaunchedAndAuthFinished_reachTheController() = runTest {
        val viewModel = lockedViewModel()

        viewModel.onEvent(MainEvent.PromptLaunched)
        assertFalse(viewModel.uiState.value.isPromptRequested)

        viewModel.onEvent(MainEvent.AuthFinished(AuthResult.LOCKOUT))
        assertEquals(LockMessage.LOCKOUT, viewModel.uiState.value.lockMessage)
        assertTrue(viewModel.uiState.value.isLocked)
    }

    @Test
    fun unlockClicked_reachesTheController() = runTest {
        val viewModel = lockedViewModel()
        viewModel.onEvent(MainEvent.PromptLaunched)
        viewModel.onEvent(MainEvent.AuthFinished(AuthResult.CANCELED))

        viewModel.onEvent(MainEvent.UnlockClicked)

        assertTrue(viewModel.uiState.value.isPromptRequested)
        assertNull(viewModel.uiState.value.lockMessage)
    }

    @Test
    fun backgroundedAndForegrounded_reachTheController() = runTest {
        val viewModel = unlockedViewModel(LockTimeout.SECONDS_30)
        assertFalse(viewModel.uiState.value.isLocked)

        viewModel.backgroundFor(30_000)

        assertTrue(viewModel.uiState.value.isLocked)
        assertTrue(viewModel.uiState.value.isPromptRequested)
    }

    @Test
    fun aChangeToTheSettings_reachesTheUiState() = runTest {
        val appLockRepository = FakeAppLockRepository(AppLockSettings(true, LockTimeout.IMMEDIATELY))
        val viewModel = mainViewModel(appLockRepository = appLockRepository)

        appLockRepository.setSettings(AppLockSettings(enabled = false))

        assertFalse(viewModel.uiState.value.isLocked)
        assertFalse(viewModel.uiState.value.isAppLockEnabled)
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

        assertEquals(listOf(lunch, rent), repository.current.sortedBy { it.id.value })
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

    override suspend fun insertCategory(category: Category): Outcome<CategoryId> = Outcome.Success(category.id)

    override suspend fun updateCategory(category: Category): Outcome<Unit> = Outcome.Success(Unit)

    override suspend fun renameBuiltInCategories(namesById: Map<CategoryId, String>): Outcome<Unit> =
        Outcome.Success(Unit)

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
