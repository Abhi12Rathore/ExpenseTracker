package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.fakes.FakeCurrencyProvider
import com.example.expensetrackerpro.utils.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountGroupingTransformationTest {

    private val transformation = AmountGroupingTransformation(CurrencyFormatter(FakeCurrencyProvider(), FakeAppLocaleProvider()))

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
}
