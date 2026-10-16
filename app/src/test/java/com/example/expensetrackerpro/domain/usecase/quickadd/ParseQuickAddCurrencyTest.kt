package com.example.expensetrackerpro.domain.usecase.quickadd

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.FieldConfidence.GUESS
import com.example.expensetrackerpro.domain.models.FieldConfidence.MISSING
import com.example.expensetrackerpro.domain.models.FieldConfidence.SURE
import com.example.expensetrackerpro.domain.models.QuickAddField
import com.example.expensetrackerpro.domain.models.QuickAddResult
import com.example.expensetrackerpro.domain.models.TransactionType.EXPENSE
import com.example.expensetrackerpro.fakes.FakeCurrencyProvider
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/** The app currency's symbol marks the amount, wherever and however it is written. */
class ParseQuickAddCurrencyTest {

    private val currencyProvider = FakeCurrencyProvider()

    private val parseQuickAdd = ParseQuickAddUseCase(
        RuleBasedQuickAddParser(),
        FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 8, hour = 12)),
        currencyProvider
    )

    private fun parse(text: String, currency: AppCurrency): QuickAddResult {
        currencyProvider.currency = currency
        return parseQuickAdd(text, EXPENSE, emptyList(), emptyList())
    }

    @Test
    fun symbolBeforeTheNumber_marksIt() {
        val result = parse("2 pizzas €600", AppCurrency.EUR)

        assertEquals(rupees(600), result.draft.amount)
        assertEquals(SURE, result.confidenceOf(QuickAddField.AMOUNT))
    }

    @Test
    fun symbolAfterTheNumber_marksIt() {
        val result = parse("taxi 30 € and 2 coffees", AppCurrency.EUR)

        assertEquals(rupees(30), result.draft.amount)
        assertEquals(SURE, result.confidenceOf(QuickAddField.AMOUNT))
    }

    @Test
    fun symbolOfLettersAndAMark_marksIt_andIsNotTheTitle() {
        val result = parse("hawker 2 plates S$12", AppCurrency.SGD)

        assertEquals(rupees(12), result.draft.amount)
        assertEquals(SURE, result.confidenceOf(QuickAddField.AMOUNT))
        assertEquals("Hawker Plates", result.draft.title)
    }

    @Test
    fun symbolOfLetters_inAnyCase_marksIt_andIsNotTheTitle() {
        val result = parse("aed 45 2 coffees", AppCurrency.AED)

        assertEquals(rupees(45), result.draft.amount)
        assertEquals(SURE, result.confidenceOf(QuickAddField.AMOUNT))
        assertEquals("Coffees", result.draft.title)
    }

    @Test
    fun anotherCurrencysSymbol_isNoMark() {
        val result = parse("2 pizzas €600 table 12", AppCurrency.USD)

        assertEquals(rupees(600), result.draft.amount)
        assertEquals(GUESS, result.confidenceOf(QuickAddField.AMOUNT))
    }

    @Test
    fun theLimit_isTheSameInEveryCurrency() {
        val result = parse("car $1,000,000", AppCurrency.USD)

        assertNull(result.draft.amount)
        assertEquals(MISSING, result.confidenceOf(QuickAddField.AMOUNT))
        assertEquals(rupees(999_999), parse("car $999,999", AppCurrency.USD).draft.amount)
    }
}
