package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.R
import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CurrencyChange
import com.example.expensetrackerpro.domain.models.CurrencyChangeExample
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.currency.ChangeCurrencyUseCase
import com.example.expensetrackerpro.domain.usecase.currency.GetCurrencyChangeExampleUseCase
import com.example.expensetrackerpro.domain.usecase.currency.GetCurrencyOptionsUseCase
import com.example.expensetrackerpro.domain.usecase.currency.GetCurrencyPreviewUseCase
import com.example.expensetrackerpro.domain.usecase.currency.SearchCurrenciesUseCase
import com.example.expensetrackerpro.domain.usecase.currency.UndoCurrencyChangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.fakes.FakeCurrencyNames
import com.example.expensetrackerpro.fakes.FakeCurrencyRepository
import com.example.expensetrackerpro.fakes.FakeDashboardRepository
import com.example.expensetrackerpro.fakes.FakeRegionCurrency
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.getDashboardUseCase
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.presentation.currency.CurrencyViewModel
import com.example.expensetrackerpro.presentation.currency.uistate.CurrencyEvent
import com.example.expensetrackerpro.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class CurrencyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val currencies = FakeCurrencyRepository(AppCurrency.INR)
    private val region = FakeRegionCurrency(AppCurrency.USD)

    // Thursday 8 Oct 2026
    private val timeProvider = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 8, hour = 19))
    private val transactions = FakeTransactionRepository(
        listOf(
            transaction(id = 1, amount = rupees(1000), type = TransactionType.INCOME, dateMillis = millisAt(2026, Calendar.OCTOBER, 1)),
            transaction(id = 2, amount = rupees(450), type = TransactionType.EXPENSE, dateMillis = millisAt(2026, Calendar.OCTOBER, 7)),
            transaction(id = 3, amount = rupees(400), type = TransactionType.EXPENSE, dateMillis = millisAt(2026, Calendar.SEPTEMBER, 30))
        )
    )
    private val dashboard = FakeDashboardRepository().apply {
        categoryBreakdown.value = listOf(CategoryBreakdown(CategoryId(1), "Fuel", CategoryIcon.LOCAL_GAS_STATION, rupees(450)))
    }

    private fun viewModel(
        savedState: SavedStateHandle = SavedStateHandle(),
        transactionRepository: FakeTransactionRepository = transactions
    ) = CurrencyViewModel(
        GetCurrencyOptionsUseCase(currencies, region, FakeCurrencyNames(), FakeAppLocaleProvider()),
        GetCurrencyPreviewUseCase(
            getDashboardUseCase(transactionRepository, dashboard, timeProvider),
            GetCurrentPeriodUseCase(timeProvider)
        ),
        SearchCurrenciesUseCase(FakeCurrencyNames(), FakeAppLocaleProvider()),
        GetCurrencyChangeExampleUseCase(transactionRepository),
        ChangeCurrencyUseCase(currencies),
        savedState
    )

    private fun CurrencyViewModel.select(currency: AppCurrency) = onEvent(CurrencyEvent.CurrencySelected(currency))

    @Test
    fun opensOnTheCurrentCurrency_withThePhoneRegionsSuggested() {
        val state = viewModel().uiState.value

        assertEquals(AppCurrency.INR, state.current)
        assertEquals(AppCurrency.INR, state.selected)
        assertEquals(listOf(AppCurrency.INR, AppCurrency.USD), state.suggested)
        assertEquals(AppCurrency.entries.size - 2, state.others.size)
        assertFalse(state.others.contains(AppCurrency.INR) || state.others.contains(AppCurrency.USD))
        // Nothing to apply until another currency is picked
        assertFalse(state.isApplyVisible)
    }

    @Test
    fun theOthers_areInOrderOfTheirNames() {
        val names = viewModel().uiState.value.others.map { FakeCurrencyNames.english.getValue(it) }

        assertEquals(names.sorted(), names)
        assertEquals("Australian Dollar", names.first())
    }

    @Test
    fun whenTheRegionsCurrencyIsTheCurrentOne_orNone_onlyTheCurrentIsSuggested() {
        region.currency = AppCurrency.INR
        assertEquals(listOf(AppCurrency.INR), viewModel().uiState.value.suggested)

        region.currency = null
        assertEquals(listOf(AppCurrency.INR), viewModel().uiState.value.suggested)
    }

    @Test
    fun opensWithThisMonthsFigures_andTheBiggestCategory() {
        val preview = requireNotNull(viewModel().uiState.value.preview)

        assertEquals(rupees(550), preview.summary.totalBalance)
        assertEquals("Fuel", preview.topCategory?.categoryName)
    }

    @Test
    fun selecting_changesOnlyTheSelection_neverTheList_orThePreviewsFigures() {
        val viewModel = viewModel()
        val before = viewModel.uiState.value

        viewModel.select(AppCurrency.EUR)

        val after = viewModel.uiState.value
        assertEquals(AppCurrency.EUR, after.selected)
        assertEquals(before.suggested, after.suggested)
        assertEquals(before.others, after.others)
        assertEquals(before.preview, after.preview)
        assertTrue(after.isApplyVisible)
        // Nothing is changed until it is applied
        assertEquals(AppCurrency.INR, currencies.currency.value)
    }

    @Test
    fun selectingTheCurrentOneAgain_hidesTheApplyButton() {
        val viewModel = viewModel()
        viewModel.select(AppCurrency.EUR)

        viewModel.select(AppCurrency.INR)

        assertFalse(viewModel.uiState.value.isApplyVisible)
    }

    @Test
    fun searching_leavesOutWhatDoesntMatch_andKeepsTheOrderOfTheRest() {
        val viewModel = viewModel()
        val othersBefore = viewModel.uiState.value.others

        viewModel.onEvent(CurrencyEvent.QueryChanged("dollar"))

        val state = viewModel.uiState.value
        assertEquals(listOf(AppCurrency.USD), state.visibleSuggested)
        assertEquals(othersBefore.filter { "Dollar" in FakeCurrencyNames.english.getValue(it) }, state.visibleOthers)
        assertEquals(othersBefore, state.others)

        viewModel.onEvent(CurrencyEvent.QueryChanged("jpy"))
        assertEquals(listOf(AppCurrency.JPY), viewModel.uiState.value.visibleOthers)

        viewModel.onEvent(CurrencyEvent.QueryChanged(""))
        assertEquals(othersBefore, viewModel.uiState.value.visibleOthers)
    }

    @Test
    fun apply_withTransactions_asksFirst_withTheLatestOneAsTheExample() = runTest {
        val viewModel = viewModel()
        viewModel.select(AppCurrency.USD)

        viewModel.onEvent(CurrencyEvent.ApplyClicked)

        assertEquals(CurrencyChangeExample(transactionCount = 3, latestAmount = rupees(450)), viewModel.uiState.value.confirmation)
        assertEquals(AppCurrency.INR, currencies.currency.value)
        assertFalse(viewModel.uiState.value.isApplied)
    }

    @Test
    fun confirming_changesTheCurrency_offersAnUndo_andCloses() = runTest {
        val viewModel = viewModel()
        viewModel.select(AppCurrency.USD)
        viewModel.onEvent(CurrencyEvent.ApplyClicked)

        viewModel.onEvent(CurrencyEvent.ConfirmClicked)

        assertEquals(AppCurrency.USD, currencies.currency.value)
        assertEquals(CurrencyChange(previous = AppCurrency.INR, current = AppCurrency.USD), currencies.pendingChange.value)
        assertTrue(viewModel.uiState.value.isApplied)
        assertNull(viewModel.uiState.value.confirmation)
    }

    @Test
    fun aChangeThatCantBeStored_showsWhy_andStaysOpenOnTheSelection() = runTest {
        currencies.failWrites = true
        val viewModel = viewModel()
        viewModel.select(AppCurrency.USD)
        viewModel.onEvent(CurrencyEvent.ApplyClicked)

        viewModel.onEvent(CurrencyEvent.ConfirmClicked)

        val state = viewModel.uiState.value
        assertEquals(R.string.error_storage_failure, state.errorRes)
        assertNull(state.confirmation)
        assertFalse(state.isApplied)
        assertEquals(AppCurrency.USD, state.selected)
        assertTrue(state.isApplyVisible)
        assertEquals(AppCurrency.INR, currencies.currency.value)
        assertNull(currencies.pendingChange.value)

        viewModel.onEvent(CurrencyEvent.ErrorShown)
        assertNull(viewModel.uiState.value.errorRes)

        // Tried again once the file can be written
        currencies.failWrites = false
        viewModel.onEvent(CurrencyEvent.ApplyClicked)
        viewModel.onEvent(CurrencyEvent.ConfirmClicked)
        assertTrue(viewModel.uiState.value.isApplied)
    }

    @Test
    fun aChangeWithoutTransactionsThatCantBeStored_showsWhyToo() = runTest {
        currencies.failWrites = true
        val viewModel = viewModel(transactionRepository = FakeTransactionRepository())
        viewModel.select(AppCurrency.EUR)

        viewModel.onEvent(CurrencyEvent.ApplyClicked)

        assertEquals(R.string.error_storage_failure, viewModel.uiState.value.errorRes)
        assertFalse(viewModel.uiState.value.isApplied)
    }

    @Test
    fun cancellingTheConfirmation_changesNothing() = runTest {
        val viewModel = viewModel()
        viewModel.select(AppCurrency.USD)
        viewModel.onEvent(CurrencyEvent.ApplyClicked)

        viewModel.onEvent(CurrencyEvent.ConfirmDismissed)

        assertNull(viewModel.uiState.value.confirmation)
        assertFalse(viewModel.uiState.value.isApplied)
        assertEquals(AppCurrency.INR, currencies.currency.value)
        // The selection stays, so the button can be tapped again
        assertTrue(viewModel.uiState.value.isApplyVisible)
    }

    @Test
    fun apply_withoutTransactions_changesAtOnce_withoutAsking() = runTest {
        val viewModel = viewModel(transactionRepository = FakeTransactionRepository())
        viewModel.select(AppCurrency.EUR)

        viewModel.onEvent(CurrencyEvent.ApplyClicked)

        assertNull(viewModel.uiState.value.confirmation)
        assertTrue(viewModel.uiState.value.isApplied)
        assertEquals(AppCurrency.EUR, currencies.currency.value)
    }

    @Test
    fun applyAndConfirm_doNothing_whenTheCurrentOneIsSelected() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(CurrencyEvent.ApplyClicked)
        viewModel.onEvent(CurrencyEvent.ConfirmClicked)

        assertNull(viewModel.uiState.value.confirmation)
        assertFalse(viewModel.uiState.value.isApplied)
        assertNull(currencies.pendingChange.value)
    }

    @Test
    fun undo_afterApplying_bringsTheCurrencyBack() = runTest {
        val viewModel = viewModel(transactionRepository = FakeTransactionRepository())
        viewModel.select(AppCurrency.EUR)
        viewModel.onEvent(CurrencyEvent.ApplyClicked)

        UndoCurrencyChangeUseCase(currencies)()

        assertEquals(AppCurrency.INR, currencies.currency.value)
        assertNull(currencies.pendingChange.value)
    }

    @Test
    fun leavingWithoutApplying_changesNothing() {
        val viewModel = viewModel()
        viewModel.select(AppCurrency.JPY)

        // Back: the screen is closed and its view model cleared without an event
        assertEquals(AppCurrency.INR, currencies.currency.value)
        assertNull(currencies.pendingChange.value)
    }

    @Test
    fun theSelectionAndSearch_surviveTheProcessBeingKilled() {
        val savedState = SavedStateHandle()
        val first = viewModel(savedState)
        first.select(AppCurrency.JPY)
        first.onEvent(CurrencyEvent.QueryChanged("yen"))

        val restored = viewModel(savedState).uiState.value

        assertEquals(AppCurrency.JPY, restored.selected)
        assertEquals("yen", restored.query)
        assertEquals(listOf(AppCurrency.JPY), restored.visibleOthers)
    }

    @Test
    fun aSavedCodeTheAppNoLongerOffers_opensOnTheCurrentCurrency() {
        val restored = viewModel(SavedStateHandle(mapOf("selected_currency" to "KWD"))).uiState.value

        assertEquals(AppCurrency.INR, restored.selected)
    }
}
