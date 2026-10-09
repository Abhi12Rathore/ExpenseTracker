package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.AmountError
import com.example.expensetrackerpro.domain.models.AmountValidation
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ValidateAmountUseCaseTest {

    private val validate = ValidateAmountUseCase()

    private fun assertValid(expected: String, input: String) {
        val result = validate(input)
        check(result is AmountValidation.Valid) { "$input was $result" }
        assertEquals(0, BigDecimal(expected).compareTo(result.amount))
    }

    private fun assertInvalid(reason: AmountError, vararg inputs: String) {
        inputs.forEach { input ->
            assertEquals(input, AmountValidation.Invalid(reason), validate(input))
        }
    }

    @Test
    fun wholeAndDecimalAmounts_areValid() {
        assertValid("4201", "4201")
        assertValid("4201.5", "4201.5")
        assertValid("0.01", "0.01")
        assertValid("250.50", "250.50")
    }

    @Test
    fun missingDigitsAroundThePoint_readAsZero() {
        assertValid("12", "12.")
        assertValid("0.5", ".5")
    }

    @Test
    fun oneCrore_isTheLargestValidAmount() {
        assertValid("10000000", "10000000")
        assertValid("10000000", "10000000.00")
        assertInvalid(AmountError.ABOVE_LIMIT, "10000000.01", "10000001", "99999999999999999999999")
    }

    @Test
    fun emptyInput_isEmpty() {
        assertInvalid(AmountError.EMPTY, "")
    }

    @Test
    fun zero_isNotPositive() {
        assertInvalid(AmountError.NOT_POSITIVE, "0", "0.00", "000", "0.")
    }

    @Test
    fun moreThanTwoDecimals_isRejected() {
        assertInvalid(AmountError.TOO_MANY_DECIMALS, "1.234", "0.001")
    }

    @Test
    fun valuesDoubleParsingWouldAccept_areMalformed() {
        assertInvalid(
            AmountError.MALFORMED,
            "NaN", "Infinity", "-Infinity", "1e5", "1E5", "1e-2", "0x10", "1f", "1d", "+5", "-5"
        )
    }

    @Test
    fun pastedJunk_isMalformed() {
        assertInvalid(
            AmountError.MALFORMED,
            "abc", "12a", "1,000", "₹12", "12 ", " 12", "1 2", "1.2.3", ".", "१२", "12\n"
        )
    }
}
