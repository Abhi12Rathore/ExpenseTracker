package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.domain.models.BuildInfo
import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.DayPeriod
import com.example.expensetrackerpro.domain.models.LanguageChange
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.language.GetLanguageOptionsUseCase
import com.example.expensetrackerpro.domain.usecase.language.GetLanguagePreviewUseCase
import com.example.expensetrackerpro.domain.usecase.language.ResolveAppLocaleUseCase
import com.example.expensetrackerpro.domain.usecase.language.SetAppLanguageUseCase
import com.example.expensetrackerpro.domain.usecase.language.UndoLanguageChangeUseCase
import com.example.expensetrackerpro.fakes.FakeAppLanguageRepository
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.FakeDashboardRepository
import com.example.expensetrackerpro.fakes.FakeDefaultCategoryNames
import com.example.expensetrackerpro.fakes.FakeDeviceLanguages
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.FakeUserProfileRepository
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.fakes.getDashboardUseCase
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.presentation.language.LanguageViewModel
import com.example.expensetrackerpro.presentation.language.uistate.LanguageEvent
import com.example.expensetrackerpro.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class LanguageViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val english = LanguageChoice.Specific(AppLanguage.ENGLISH)
    private val hindi = LanguageChoice.Specific(AppLanguage.HINDI)

    // The phone in English (India), with Hindi as its second language
    private val deviceLanguages = FakeDeviceLanguages(listOf("en-IN", "hi-IN").map(Locale::forLanguageTag))
    private val languages = FakeAppLanguageRepository()

    // Thursday 8 Oct 2026, in the evening
    private val timeProvider = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 8, hour = 19))
    private val transactions = FakeTransactionRepository(
        listOf(
            transaction(id = 1, amount = rupees(1000), type = TransactionType.INCOME, dateMillis = millisAt(2026, Calendar.OCTOBER, 1)),
            transaction(id = 2, amount = rupees(250), type = TransactionType.EXPENSE, dateMillis = millisAt(2026, Calendar.OCTOBER, 7)),
            // Last month's: not part of the preview
            transaction(id = 3, amount = rupees(400), type = TransactionType.EXPENSE, dateMillis = millisAt(2026, Calendar.SEPTEMBER, 30))
        )
    )
    private val dashboard = FakeDashboardRepository().apply {
        categoryBreakdown.value = listOf(
            CategoryBreakdown(CategoryId(1), "Fuel", CategoryIcon.LOCAL_GAS_STATION, rupees(150)),
            CategoryBreakdown(CategoryId(2), "Office cab", CategoryIcon.LOCAL_TAXI, rupees(70)),
            CategoryBreakdown(CategoryId(3), "Pets", CategoryIcon.PETS, rupees(30))
        )
    }
    private val categories = FakeCategoryRepository(
        listOf(
            category(id = 1, name = "Fuel").copy(isCustom = false, builtInKey = BuiltInCategory.FUEL),
            // A built-in the user renamed
            category(id = 2, name = "Office cab").copy(isCustom = false),
            category(id = 3, name = "Pets")
        )
    )

    private val resolveAppLocale = ResolveAppLocaleUseCase()

    private fun viewModel() = LanguageViewModel(
        GetLanguageOptionsUseCase(languages, deviceLanguages, resolveAppLocale),
        GetLanguagePreviewUseCase(
            getDashboardUseCase(transactions, dashboard, timeProvider, FakeUserProfileRepository("Abhishek")),
            GetCurrentPeriodUseCase(timeProvider),
            GetCategoriesUseCase(categories),
            FakeDefaultCategoryNames(),
            deviceLanguages,
            resolveAppLocale
        ),
        SetAppLanguageUseCase(languages, deviceLanguages, resolveAppLocale),
        BuildInfo(isDebug = false, versionName = "1.4")
    )

    private fun LanguageViewModel.select(choice: LanguageChoice) = onEvent(LanguageEvent.LanguageSelected(choice))

    @Test
    fun opensOnTheCurrentLanguage_withThePhonesOtherLanguagesSuggested() {
        val state = viewModel().uiState.value

        assertEquals(LanguageChoice.SystemDefault, state.current)
        assertEquals(LanguageChoice.SystemDefault, state.selected)
        assertEquals("English (India)", state.systemLanguageName)
        assertEquals(listOf(AppLanguage.HINDI), state.suggested)
        assertEquals(AppLanguage.entries - AppLanguage.HINDI, state.others)
        assertEquals("1.4", state.versionName)
        // Nothing to apply or preview until another language is picked
        assertFalse(state.isApplyVisible)
        assertNull(state.preview)
        assertFalse(state.isApplied)
    }

    @Test
    fun opensOnALanguageThatWasChosenBefore() {
        languages.stored = hindi

        val state = viewModel().uiState.value

        assertEquals(hindi, state.current)
        assertEquals(hindi, state.selected)
        assertEquals(AppLanguage.HINDI, state.selectedLanguage)
    }

    @Test
    fun selectingALanguage_offersIt_andPreviewsTheUsersOwnMonthInIt() = runTest {
        val viewModel = viewModel()

        viewModel.select(hindi)

        val state = viewModel.uiState.value
        assertEquals(hindi, state.selected)
        assertEquals(AppLanguage.HINDI, state.selectedLanguage)
        assertTrue(state.isApplyVisible)
        // The app itself has not changed
        assertEquals(LanguageChoice.SystemDefault, languages.stored)

        val preview = requireNotNull(state.preview)
        assertEquals(AppLanguage.HINDI, preview.language)
        assertEquals("Abhishek", preview.userName)
        assertEquals(DayPeriod.EVENING, preview.dayPeriod)
        assertEquals(rupees(1000), preview.summary.totalIncome)
        assertEquals(rupees(250), preview.summary.totalExpense)
        assertEquals(rupees(750), preview.summary.totalBalance)
        // The two biggest; only the untouched built-in is named in Hindi
        assertEquals(listOf("ईंधन", "Office cab"), preview.categories.map { it.categoryName })
        assertEquals(listOf(rupees(150), rupees(70)), preview.categories.map { it.amount })
    }

    @Test
    fun selectingTheCurrentLanguageAgain_hidesTheButtonAndThePreview() = runTest {
        val viewModel = viewModel()
        viewModel.select(hindi)

        viewModel.select(LanguageChoice.SystemDefault)

        val state = viewModel.uiState.value
        assertFalse(state.isApplyVisible)
        assertNull(state.preview)
    }

    @Test
    fun selectingEnglishWhileFollowingAnEnglishPhone_isStillAChange_previewedInEnglish() = runTest {
        val viewModel = viewModel()

        viewModel.select(english)

        val state = viewModel.uiState.value
        assertTrue(state.isApplyVisible)
        assertEquals(AppLanguage.ENGLISH, state.preview?.language)
        assertEquals(listOf("Fuel", "Office cab"), state.preview?.categories?.map { it.categoryName })
    }

    @Test
    fun applying_closesTheScreenFirst_thenSwitchesTheApp_andCanBeUndone() = runTest {
        val viewModel = viewModel()
        viewModel.select(hindi)

        viewModel.onEvent(LanguageEvent.ApplyClicked)

        assertTrue(viewModel.uiState.value.isApplied)
        // Not yet: the screen closes before the app is drawn again
        assertEquals(LanguageChoice.SystemDefault, languages.stored)

        viewModel.onEvent(LanguageEvent.ClosedAfterApply)

        assertEquals(hindi, languages.stored)
        assertEquals(
            LanguageChange(previous = LanguageChoice.SystemDefault, language = AppLanguage.HINDI),
            languages.pendingChange.value
        )

        UndoLanguageChangeUseCase(languages)()

        assertEquals(LanguageChoice.SystemDefault, languages.stored)
        assertNull(languages.pendingChange.value)
    }

    @Test
    fun applyingWithNothingNewSelected_doesNothing() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(LanguageEvent.ApplyClicked)
        viewModel.onEvent(LanguageEvent.ClosedAfterApply)

        assertFalse(viewModel.uiState.value.isApplied)
        assertEquals(LanguageChoice.SystemDefault, languages.stored)
        assertNull(languages.pendingChange.value)
    }

    @Test
    fun closingWithoutApplying_leavesTheLanguageAlone() = runTest {
        val viewModel = viewModel()
        viewModel.select(hindi)

        // Only a confirmed selection is ever applied
        viewModel.onEvent(LanguageEvent.ClosedAfterApply)

        assertEquals(LanguageChoice.SystemDefault, languages.stored)
    }
}
