package com.example.expensetrackerpro.domain.models

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * An amount of rupees held as whole paise, so sums and differences are exact. May be negative:
 * a balance or a difference can be.
 */
@JvmInline
value class Money(val paise: Long) : Comparable<Money> {

    val isPositive: Boolean get() = paise > 0
    val isNegative: Boolean get() = paise < 0
    val isZero: Boolean get() = paise == 0L

    operator fun plus(other: Money): Money = Money(paise + other.paise)

    operator fun minus(other: Money): Money = Money(paise - other.paise)

    operator fun unaryMinus(): Money = Money(-paise)

    operator fun times(factor: Int): Money = Money(paise * factor)

    /** An equal share of this amount, to the nearest paisa with halves rounded away from zero. */
    operator fun div(parts: Int): Money = Money(divideRounding(paise, parts.toLong()))

    override fun compareTo(other: Money): Int = paise.compareTo(other.paise)

    fun abs(): Money = if (isNegative) -this else this

    /** To the nearest whole rupee, halves rounded away from zero. */
    fun roundedToRupees(): Money = Money(divideRounding(paise, PAISE_PER_RUPEE) * PAISE_PER_RUPEE)

    /**
     * This amount as a fraction of [whole]: 0.25 for ₹25 of ₹100. 0 when [whole] is zero. For
     * percentages and chart geometry only; the result is never turned back into an amount.
     */
    fun ratioTo(whole: Money): Float =
        if (whole.isZero) 0f else (paise.toDouble() / whole.paise.toDouble()).toFloat()

    companion object {
        const val PAISE_PER_RUPEE = 100L

        val ZERO = Money(0)

        fun ofRupees(rupees: Long): Money = Money(rupees * PAISE_PER_RUPEE)

        /** [rupees] to the nearest paisa, halves rounded up. */
        fun fromRupees(rupees: BigDecimal): Money =
            Money(rupees.setScale(DECIMALS, RoundingMode.HALF_UP).movePointRight(DECIMALS).longValueExact())

        private const val DECIMALS = 2

        private fun divideRounding(value: Long, divisor: Long): Long {
            val half = divisor / 2
            return if (value >= 0) (value + half) / divisor else (value - half) / divisor
        }
    }
}

fun Iterable<Money>.sum(): Money = Money(sumOf { it.paise })

/** The total of what [selector] gives for each element. */
inline fun <T> Iterable<T>.sumOfMoney(selector: (T) -> Money): Money = Money(sumOf { selector(it).paise })
