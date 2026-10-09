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

    @Test
    fun formatWhole_roundsToRupees_withIndianGrouping() {
        assertEquals("₹0", CurrencyFormatter.formatWhole(0.0))
        assertEquals("₹3,200", CurrencyFormatter.formatWhole(3200.0))
        assertEquals("₹27,550", CurrencyFormatter.formatWhole(27549.5))
        assertEquals("₹1,00,000", CurrencyFormatter.formatWhole(99999.6))
    }

    @Test
    fun formatWhole_negative_putsSignBeforeSymbol_unlessItRoundsToZero() {
        assertEquals("-₹1,250", CurrencyFormatter.formatWhole(-1250.0))
        assertEquals("₹0", CurrencyFormatter.formatWhole(-0.4))
    }

    @Test
    fun formatShort_belowAThousand_isWholeRupees() {
        assertEquals("₹0", CurrencyFormatter.formatShort(0.0))
        assertEquals("₹950", CurrencyFormatter.formatShort(950.0))
        assertEquals("₹999", CurrencyFormatter.formatShort(999.4))
    }

    @Test
    fun formatShort_thousands_useK_withAtMostOneDecimal() {
        assertEquals("₹1k", CurrencyFormatter.formatShort(1000.0))
        assertEquals("₹2.4k", CurrencyFormatter.formatShort(2400.0))
        assertEquals("₹2.3k", CurrencyFormatter.formatShort(2340.0))
        assertEquals("₹12k", CurrencyFormatter.formatShort(12000.0))
        assertEquals("₹99.9k", CurrencyFormatter.formatShort(99940.0))
    }

    @Test
    fun formatShort_lakhsAndCrores_useIndianUnits() {
        assertEquals("₹1L", CurrencyFormatter.formatShort(100000.0))
        assertEquals("₹1.5L", CurrencyFormatter.formatShort(150000.0))
        assertEquals("₹25L", CurrencyFormatter.formatShort(2500000.0))
        assertEquals("₹1Cr", CurrencyFormatter.formatShort(10000000.0))
        assertEquals("₹2.5Cr", CurrencyFormatter.formatShort(25000000.0))
    }

    @Test
    fun formatShort_roundingUpToTheNextUnit_switchesUnit() {
        assertEquals("₹1k", CurrencyFormatter.formatShort(999.6))
        assertEquals("₹1L", CurrencyFormatter.formatShort(99960.0))
        assertEquals("₹1Cr", CurrencyFormatter.formatShort(9996000.0))
    }

    @Test
    fun formatShort_negative_putsSignBeforeSymbol() {
        assertEquals("-₹2.4k", CurrencyFormatter.formatShort(-2400.0))
        assertEquals("₹0", CurrencyFormatter.formatShort(-0.2))
    }
}
