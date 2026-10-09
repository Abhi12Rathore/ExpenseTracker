package com.example.expensetrackerpro.domain.usecase.transaction

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatAmountInputUseCaseTest {

    private val format = FormatAmountInputUseCase()

    @Test
    fun wholeAmount_hasNoDecimalPoint() {
        assertEquals("4201", format(4201.0))
        assertEquals("100", format(100.0))
        assertEquals("10000000", format(10_000_000.0))
    }

    @Test
    fun decimals_areShownWithoutTrailingZeros() {
        assertEquals("4201.5", format(4201.5))
        assertEquals("4201.25", format(4201.25))
        assertEquals("0.1", format(0.1))
    }

    @Test
    fun moreThanTwoDecimals_areRounded() {
        assertEquals("1.24", format(1.235))
        assertEquals("2", format(1.999))
    }

    @Test
    fun whatItShows_isAValidAmount() {
        val sanitize = SanitizeAmountInputUseCase()

        listOf(4201.0, 4201.5, 0.01, 10_000_000.0).forEach { amount ->
            assertEquals(format(amount), sanitize("", format(amount)))
        }
    }
}
