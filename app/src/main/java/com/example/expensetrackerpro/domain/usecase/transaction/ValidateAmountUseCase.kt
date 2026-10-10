package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.AmountError
import com.example.expensetrackerpro.domain.models.AmountValidation
import com.example.expensetrackerpro.domain.models.Money
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Reads an amount typed as plain digits with an optional decimal point: more than zero, at most
 * two decimals and at most [MAX_AMOUNT]. Anything else (signs, exponents, `NaN`, grouping commas,
 * currency symbols, spaces) is malformed.
 */
class ValidateAmountUseCase @Inject constructor() {

    operator fun invoke(input: String): AmountValidation {
        if (input.isEmpty()) return AmountValidation.Invalid(AmountError.EMPTY)

        val match = PATTERN.matchEntire(input)
        val integerPart = match?.groupValues?.get(1).orEmpty()
        val decimals = match?.groupValues?.get(2).orEmpty()
        if (match == null || (integerPart.isEmpty() && decimals.isEmpty())) {
            return AmountValidation.Invalid(AmountError.MALFORMED)
        }
        if (decimals.length > MAX_DECIMALS) {
            return AmountValidation.Invalid(AmountError.TOO_MANY_DECIMALS)
        }

        val amount = BigDecimal("${integerPart.ifEmpty { "0" }}.${decimals.ifEmpty { "0" }}")
        return when {
            amount.signum() <= 0 -> AmountValidation.Invalid(AmountError.NOT_POSITIVE)
            amount > MAX_AMOUNT -> AmountValidation.Invalid(AmountError.ABOVE_LIMIT)
            else -> AmountValidation.Valid(Money.fromRupees(amount))
        }
    }

    companion object {
        /** One crore. */
        val MAX_AMOUNT: BigDecimal = BigDecimal(10_000_000)
        const val MAX_DECIMALS = 2

        // ASCII digits only, so digits from other scripts are not accepted
        private val PATTERN = Regex("""([0-9]*)(?:\.([0-9]*))?""")
    }
}
