package com.example.expensetrackerpro.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun ofRupees_isAHundredPaiseEach() {
        assertEquals(Money(1_250_00), Money.ofRupees(1250))
        assertEquals(Money(-5_00), Money.ofRupees(-5))
    }

    @Test
    fun fromRupees_keepsTwoDecimalsExactly() {
        assertEquals(Money(4201_50), Money.fromRupees(BigDecimal("4201.5")))
        assertEquals(Money(1), Money.fromRupees(BigDecimal("0.01")))
        assertEquals(Money(10_000_000_00), Money.fromRupees(BigDecimal("10000000")))
    }

    @Test
    fun fromRupees_roundsFurtherDecimalsHalfUp() {
        assertEquals(Money(1_24), Money.fromRupees(BigDecimal("1.235")))
        assertEquals(Money(1_23), Money.fromRupees(BigDecimal("1.2349")))
        assertEquals(Money(2_00), Money.fromRupees(BigDecimal("1.999")))
    }

    @Test
    fun plusAndMinus_areExact() {
        assertEquals(Money(30), Money(10) + Money(20))
        assertEquals(Money(-10), Money(10) - Money(20))
        // 0.1 + 0.2 + … as doubles drifts; paise don't
        assertEquals(Money(1_00), List(10) { Money(10) }.sum())
    }

    @Test
    fun unaryMinus_flipsTheSign() {
        assertEquals(Money(-250), -Money(250))
        assertEquals(Money(250), -Money(-250))
        assertEquals(Money.ZERO, -Money.ZERO)
    }

    @Test
    fun times_multipliesByAWholeNumber() {
        assertEquals(Money(7_50), Money(2_50) * 3)
        assertEquals(Money.ZERO, Money(2_50) * 0)
        assertEquals(Money(-5_00), Money(2_50) * -2)
    }

    @Test
    fun div_roundsToTheNearestPaisa_halvesAwayFromZero() {
        assertEquals(Money(33), Money(1_00) / 3)
        assertEquals(Money(67), Money(2_00) / 3)
        assertEquals(Money(3), Money(5) / 2)
        assertEquals(Money(-3), Money(-5) / 2)
        assertEquals(Money(-33), Money(-1_00) / 3)
    }

    @Test
    fun signChecks() {
        assertTrue(Money(1).isPositive)
        assertFalse(Money.ZERO.isPositive)
        assertTrue(Money(-1).isNegative)
        assertFalse(Money.ZERO.isNegative)
        assertTrue(Money.ZERO.isZero)
        assertFalse(Money(1).isZero)
    }

    @Test
    fun abs_dropsTheSign() {
        assertEquals(Money(250), Money(-250).abs())
        assertEquals(Money(250), Money(250).abs())
    }

    @Test
    fun comparesByAmount() {
        assertTrue(Money(1) > Money.ZERO)
        assertTrue(Money(-1) < Money.ZERO)
        assertEquals(Money(5), maxOf(Money(5), Money(-9)))
        assertEquals(listOf(Money(-2), Money(1), Money(3)), listOf(Money(3), Money(-2), Money(1)).sorted())
    }

    @Test
    fun roundedToRupees_roundsHalvesAwayFromZero() {
        assertEquals(Money.ofRupees(27_550), Money(27_549_50).roundedToRupees())
        assertEquals(Money.ofRupees(27_549), Money(27_549_49).roundedToRupees())
        assertEquals(Money.ofRupees(-1), Money(-50).roundedToRupees())
        assertEquals(Money.ZERO, Money(-49).roundedToRupees())
    }

    @Test
    fun sum_addsEveryAmount_andIsZeroForNone() {
        assertEquals(Money(60), listOf(Money(10), Money(20), Money(30)).sum())
        assertEquals(Money(-5), listOf(Money(10), Money(-15)).sum())
        assertEquals(Money.ZERO, emptyList<Money>().sum())
    }

    @Test
    fun sumOfMoney_addsWhatTheSelectorGives() {
        val prices = listOf("tea" to Money(15_00), "samosa" to Money(20_50))

        assertEquals(Money(35_50), prices.sumOfMoney { it.second })
        assertEquals(Money.ZERO, emptyList<Pair<String, Money>>().sumOfMoney { it.second })
    }

    @Test
    fun ratioTo_isThePartOfTheWhole() {
        assertEquals(0.25f, Money.ofRupees(25).ratioTo(Money.ofRupees(100)), 0f)
        assertEquals(1f, Money(1).ratioTo(Money(1)), 0f)
        assertEquals(1.5f, Money(150).ratioTo(Money(100)), 0f)
        assertEquals(-0.5f, Money(-50).ratioTo(Money(100)), 0f)
        assertEquals(1f / 3f, Money(1).ratioTo(Money(3)), 0.0001f)
    }

    @Test
    fun ratioTo_aWholeOfZero_isZero() {
        assertEquals(0f, Money(500).ratioTo(Money.ZERO), 0f)
        assertEquals(0f, Money.ZERO.ratioTo(Money.ZERO), 0f)
    }
}
