package com.example.expensetrackerpro.utils

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CurrencyFormatterTest {

    private val formatter = CurrencyFormatter(currencyProvider = { AppCurrency.INR }, localeProvider = { Locale.ENGLISH })

    @Test
    fun zero() {
        assertEquals("₹0.00", formatter.format(Money.ZERO))
    }

    @Test
    fun upToThreeDigits_haveNoSeparator() {
        assertEquals("₹5.00", formatter.format(Money.ofRupees(5)))
        assertEquals("₹999.00", formatter.format(Money.ofRupees(999)))
    }

    @Test
    fun usesIndianGrouping() {
        assertEquals("₹1,000.00", formatter.format(Money.ofRupees(1000)))
        assertEquals("₹12,345.00", formatter.format(Money.ofRupees(12345)))
        assertEquals("₹1,00,000.00", formatter.format(Money.ofRupees(100000)))
        assertEquals("₹12,34,567.00", formatter.format(Money.ofRupees(1234567)))
        assertEquals("₹1,23,45,678.00", formatter.format(Money.ofRupees(12345678)))
    }

    @Test
    fun alwaysShowsTwoDecimals() {
        assertEquals("₹250.50", formatter.format(Money(250_50)))
        assertEquals("₹0.05", formatter.format(Money(5)))
        assertEquals("₹999.99", formatter.format(Money(999_99)))
    }

    @Test
    fun sumOfTenAndTwentyPaise_isExactlyThirty() {
        assertEquals("₹0.30", formatter.format(Money(10) + Money(20)))
    }

    @Test
    fun crores_areGroupedInTwos() {
        assertEquals("₹1,00,00,000.00", formatter.format(Money.ofRupees(1_00_00_000)))
        assertEquals("₹12,34,56,78,901.25", formatter.format(Money(12_34_56_78_901_25)))
    }

    @Test
    fun amountsBeyondWhatADoubleHoldsExactly_keepEveryPaisa() {
        assertEquals("₹9,00,71,99,25,47,40,993.01", formatter.format(Money(9_007_199_254_740_993_01)))
    }

    @Test
    fun negativeAmount_putsSignBeforeSymbol() {
        assertEquals("-₹1,00,000.00", formatter.format(Money.ofRupees(-100000)))
        assertEquals("-₹0.50", formatter.format(Money(-50)))
    }

    @Test
    fun onePaisaBelowZero_keepsItsSign() {
        assertEquals("-₹0.01", formatter.format(Money(-1)))
    }

    @Test
    fun formatSigned_incomeIsPlus_expenseIsMinus() {
        assertEquals("+₹1,500.00", formatter.formatSigned(Money.ofRupees(1500), isIncome = true))
        assertEquals("-₹1,500.00", formatter.formatSigned(Money.ofRupees(1500), isIncome = false))
        assertEquals("-₹0.00", formatter.formatSigned(Money.ZERO, isIncome = false))
    }

    @Test
    fun formatWhole_roundsToRupees_withIndianGrouping() {
        assertEquals("₹0", formatter.formatWhole(Money.ZERO))
        assertEquals("₹3,200", formatter.formatWhole(Money.ofRupees(3200)))
        assertEquals("₹27,550", formatter.formatWhole(Money(27549_50)))
        assertEquals("₹1,00,000", formatter.formatWhole(Money(99999_60)))
    }

    @Test
    fun formatWhole_negative_putsSignBeforeSymbol_unlessItRoundsToZero() {
        assertEquals("-₹1,250", formatter.formatWhole(Money.ofRupees(-1250)))
        assertEquals("₹0", formatter.formatWhole(Money(-40)))
    }

    @Test
    fun formatShort_belowAThousand_isWholeRupees() {
        assertEquals("₹0", formatter.formatShort(Money.ZERO))
        assertEquals("₹950", formatter.formatShort(Money.ofRupees(950)))
        assertEquals("₹999", formatter.formatShort(Money(999_40)))
    }

    @Test
    fun formatShort_thousands_useK_withAtMostOneDecimal() {
        assertEquals("₹1k", formatter.formatShort(Money.ofRupees(1000)))
        assertEquals("₹2.4k", formatter.formatShort(Money.ofRupees(2400)))
        assertEquals("₹2.3k", formatter.formatShort(Money.ofRupees(2340)))
        assertEquals("₹12k", formatter.formatShort(Money.ofRupees(12000)))
        assertEquals("₹99.9k", formatter.formatShort(Money.ofRupees(99940)))
    }

    @Test
    fun formatShort_lakhsAndCrores_useIndianUnits() {
        assertEquals("₹1L", formatter.formatShort(Money.ofRupees(100000)))
        assertEquals("₹1.5L", formatter.formatShort(Money.ofRupees(150000)))
        assertEquals("₹25L", formatter.formatShort(Money.ofRupees(2500000)))
        assertEquals("₹1Cr", formatter.formatShort(Money.ofRupees(10000000)))
        assertEquals("₹2.5Cr", formatter.formatShort(Money.ofRupees(25000000)))
    }

    @Test
    fun formatShort_roundingUpToTheNextUnit_switchesUnit() {
        assertEquals("₹1k", formatter.formatShort(Money(999_60)))
        assertEquals("₹1L", formatter.formatShort(Money.ofRupees(99960)))
        assertEquals("₹1Cr", formatter.formatShort(Money.ofRupees(9996000)))
    }

    @Test
    fun formatShort_negative_putsSignBeforeSymbol() {
        assertEquals("-₹2.4k", formatter.formatShort(Money.ofRupees(-2400)))
        assertEquals("₹0", formatter.formatShort(Money(-20)))
    }
}
