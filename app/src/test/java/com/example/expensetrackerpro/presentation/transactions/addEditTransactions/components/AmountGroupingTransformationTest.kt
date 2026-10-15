package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.fakes.FakeCurrencyProvider
import com.example.expensetrackerpro.fakes.FakeNumberSymbols
import com.example.expensetrackerpro.domain.currency.AppCurrency
import java.util.Locale
import com.example.expensetrackerpro.utils.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountGroupingTransformationTest {

    private val currencies = FakeCurrencyProvider()
    private val locales = FakeAppLocaleProvider()
    private val transformation = AmountGroupingTransformation(CurrencyFormatter(currencies, locales, FakeNumberSymbols()))

    private fun transform(typed: String): TransformedText = transformation.filter(AnnotatedString(typed))

    @Test
    fun showsTheTypedAmountGroupedAsTheCurrencyGroupsIt() {
        assertEquals("1,23,456.7", transform("123456.7").text.text)
        assertEquals("999", transform("999").text.text)
    }

    @Test
    fun emptyField_staysEmpty() {
        val transformed = transform("")

        assertEquals("", transformed.text.text)
        assertEquals(0, transformed.offsetMapping.originalToTransformed(0))
        assertEquals(0, transformed.offsetMapping.transformedToOriginal(0))
    }

    @Test
    fun decimalsOnly_areLeftAsTyped() {
        val transformed = transform(".75")

        assertEquals(".75", transformed.text.text)
        for (offset in 0..3) {
            assertEquals(offset, transformed.offsetMapping.originalToTransformed(offset))
            assertEquals(offset, transformed.offsetMapping.transformedToOriginal(offset))
        }
    }

    @Test
    fun cursorAtTheEnd_staysAtTheEnd() {
        val transformed = transform("123456.7")

        assertEquals("1,23,456.7".length, transformed.offsetMapping.originalToTransformed("123456.7".length))
        assertEquals("123456.7".length, transformed.offsetMapping.transformedToOriginal("1,23,456.7".length))
    }

    @Test
    fun eachTypedCharacter_landsOnItselfOnceSeparatorsAreAdded() {
        val typed = "1234567.89"
        val transformed = transform(typed)
        val shown = transformed.text.text

        typed.forEachIndexed { offset, char ->
            assertEquals(char, shown[transformed.offsetMapping.originalToTransformed(offset)])
        }
    }

    @Test
    fun cursorRightAfterASeparator_mapsToTheDigitBeforeIt() {
        // "1,23,456": after the first comma (shown offset 2) is after one typed digit
        val transformed = transform("123456")

        assertEquals(1, transformed.offsetMapping.transformedToOriginal(1))
        assertEquals(1, transformed.offsetMapping.transformedToOriginal(2))
        assertEquals(3, transformed.offsetMapping.transformedToOriginal(5))
    }

    @Test
    fun inGerman_euros_areGroupedWithDots_andTheDecimalIsAComma() {
        currencies.currency = AppCurrency.EUR
        locales.locale = Locale.GERMAN
        val transformed = transform("1234567.8")

        assertEquals("1.234.567,8", transformed.text.text)
        // The decimal is shown in its place, so the cursor after it stays after it
        assertEquals("1.234.567,".length, transformed.offsetMapping.originalToTransformed("1234567.".length))
        assertEquals("1234567.".length, transformed.offsetMapping.transformedToOriginal("1.234.567,".length))
    }

    @Test
    fun inFrench_theGroupingSpace_isSkippedByTheCursor() {
        currencies.currency = AppCurrency.EUR
        locales.locale = Locale.FRENCH
        val space = FakeNumberSymbols.NARROW_NO_BREAK_SPACE
        val transformed = transform("123456")

        assertEquals("123${space}456", transformed.text.text)
        assertEquals(3, transformed.offsetMapping.transformedToOriginal(3))
        assertEquals(3, transformed.offsetMapping.transformedToOriginal(4))
        assertEquals(4, transformed.offsetMapping.originalToTransformed(3))
        assertEquals(7, transformed.offsetMapping.originalToTransformed(6))
    }

    @Test
    fun dollars_areGroupedInThousands_notLakhs() {
        currencies.currency = AppCurrency.USD

        assertEquals("123,456.7", transform("123456.7").text.text)
    }
}
