package com.example.expensetrackerpro.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun zero() {
        assertEquals("₹0.00", CurrencyFormatter.format(0.0))
    }

    @Test
    fun upToThreeDigits_haveNoSeparator() {
        assertEquals("₹5.00", CurrencyFormatter.format(5.0))
        assertEquals("₹999.00", CurrencyFormatter.format(999.0))
    }

    @Test
    fun usesIndianGrouping() {
        assertEquals("₹1,000.00", CurrencyFormatter.format(1000.0))
        assertEquals("₹12,345.00", CurrencyFormatter.format(12345.0))
        assertEquals("₹1,00,000.00", CurrencyFormatter.format(100000.0))
        assertEquals("₹12,34,567.00", CurrencyFormatter.format(1234567.0))
        assertEquals("₹1,23,45,678.00", CurrencyFormatter.format(12345678.0))
    }

    @Test
    fun alwaysShowsTwoDecimals_rounded() {
        assertEquals("₹250.50", CurrencyFormatter.format(250.5))
        assertEquals("₹1.24", CurrencyFormatter.format(1.235001))
        assertEquals("₹1,000.00", CurrencyFormatter.format(999.999))
    }

    @Test
    fun negativeAmount_putsSignBeforeSymbol() {
        assertEquals("-₹1,00,000.00", CurrencyFormatter.format(-100000.0))
        assertEquals("-₹0.50", CurrencyFormatter.format(-0.5))
    }

    @Test
    fun negativeThatRoundsToZero_hasNoSign() {
        assertEquals("₹0.00", CurrencyFormatter.format(-0.001))
    }

    @Test
    fun formatSigned_incomeIsPlus_expenseIsMinus() {
        assertEquals("+₹1,500.00", CurrencyFormatter.formatSigned(1500.0, isIncome = true))
        assertEquals("-₹1,500.00", CurrencyFormatter.formatSigned(1500.0, isIncome = false))
        assertEquals("-₹0.00", CurrencyFormatter.formatSigned(0.0, isIncome = false))
    }
}
