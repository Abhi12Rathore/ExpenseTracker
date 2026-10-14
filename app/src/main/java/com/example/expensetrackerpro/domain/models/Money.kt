package com.example.expensetrackerpro.domain.models

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * An amount held as whole minor units: hundredths of the currency's unit (paise for rupees), for
 * every currency, so sums and differences are exact. May be negative: a balance or a difference can be.
 */
@JvmInline
value class Money(val minorUnits: Long) : Comparable<Money> {

    val isPositive: Boolean get() = minorUnits > 0
    val isNegative: Boolean get() = minorUnits < 0
    val isZero: Boolean get() = minorUnits == 0L

    operator fun plus(other: Money): Money = Money(minorUnits + other.minorUnits)

    operator fun minus(other: Money): Money = Money(minorUnits - other.minorUnits)

    operator fun unaryMinus(): Money = Money(-minorUnits)

    operator fun times(factor: Int): Money = Money(minorUnits * factor)

    /** An equal share of this amount, to the nearest minor unit with halves rounded away from zero. */
    operator fun div(parts: Int): Money = Money(divideRounding(minorUnits, parts.toLong()))

    override fun compareTo(other: Money): Int = minorUnits.compareTo(other.minorUnits)

    fun abs(): Money = if (isNegative) -this else this

    /** To the nearest whole unit (rupee), halves rounded away from zero. */
    fun roundedToRupees(): Money = Money(divideRounding(minorUnits, MINOR_PER_UNIT) * MINOR_PER_UNIT)

    /**
     * This amount as a fraction of [whole]: 0.25 for ₹25 of ₹100. 0 when [whole] is zero. For
     * percentages and chart geometry only; the result is never turned back into an amount.
     */
    fun ratioTo(whole: Money): Float =
        if (whole.isZero) 0f else (minorUnits.toDouble() / whole.minorUnits.toDouble()).toFloat()

    companion object {
        // Minor units in one unit of the currency, the same for every currency
        const val MINOR_PER_UNIT = 100L

        val ZERO = Money(0)

        fun ofRupees(rupees: Long): Money = Money(rupees * MINOR_PER_UNIT)

        /** [rupees] to the nearest minor unit (paisa), halves rounded up. */
        fun fromRupees(rupees: BigDecimal): Money =
            Money(rupees.setScale(DECIMALS, RoundingMode.HALF_UP).movePointRight(DECIMALS).longValueExact())

        private const val DECIMALS = 2

        private fun divideRounding(value: Long, divisor: Long): Long {
            val half = divisor / 2
            return if (value >= 0) (value + half) / divisor else (value - half) / divisor
        }
    }
}

fun Iterable<Money>.sum(): Money = Money(sumOf { it.minorUnits })

/** The total of what [selector] gives for each element. */
inline fun <T> Iterable<T>.sumOfMoney(selector: (T) -> Money): Money = Money(sumOf { selector(it).minorUnits })
