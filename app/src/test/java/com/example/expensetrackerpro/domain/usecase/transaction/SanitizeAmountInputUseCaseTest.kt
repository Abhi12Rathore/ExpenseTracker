package com.example.expensetrackerpro.domain.usecase.transaction

import org.junit.Assert.assertEquals
import org.junit.Test

class SanitizeAmountInputUseCaseTest {

    private val sanitize = SanitizeAmountInputUseCase()

    @Test
    fun digitsAndOneDecimalPoint_areKept() {
        assertEquals("1", sanitize("", "1"))
        assertEquals("12.", sanitize("12", "12."))
        assertEquals("12.5", sanitize("12.", "12.5"))
        assertEquals("12.50", sanitize("12.5", "12.50"))
    }

    @Test
    fun clearingTheField_isAllowed() {
        assertEquals("", sanitize("12", ""))
    }

    @Test
    fun thirdDecimal_isIgnored() {
        assertEquals("12.50", sanitize("12.50", "12.505"))
    }

    @Test
    fun leadingZeros_areDropped() {
        assertEquals("7", sanitize("0", "07"))
        assertEquals("0", sanitize("", "0"))
        assertEquals("0", sanitize("0", "00"))
        assertEquals("0.5", sanitize("0.", "0.5"))
    }

    @Test
    fun barePoint_getsAZeroInFront() {
        assertEquals("0.", sanitize("", "."))
        assertEquals("0.5", sanitize("", ".5"))
    }

    @Test
    fun aboveOneCrore_isIgnored() {
        assertEquals("10000000", sanitize("1000000", "10000000"))
        assertEquals("10000000", sanitize("10000000", "100000000"))
        assertEquals("10000000", sanitize("10000000", "10000000.1"))
    }

    @Test
    fun pastedJunk_leavesTheFieldAsItWas() {
        listOf("1e5", "NaN", "Infinity", "-5", "1,000", "₹500", "12 ", "abc", "1.2.3").forEach { pasted ->
            assertEquals(pasted, "42", sanitize("42", pasted))
        }
    }
}
