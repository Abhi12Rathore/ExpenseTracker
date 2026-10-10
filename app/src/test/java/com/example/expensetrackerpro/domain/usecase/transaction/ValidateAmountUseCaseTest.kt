package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.AmountError
import com.example.expensetrackerpro.domain.models.AmountValidation
import com.example.expensetrackerpro.domain.models.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateAmountUseCaseTest {

    private val validate = ValidateAmountUseCase()

    private fun assertValid(expected: Money, input: String) {
        assertEquals(input, AmountValidation.Valid(expected), validate(input))
    }

    private fun assertInvalid(reason: AmountError, vararg inputs: String) {
        inputs.forEach { input ->
            assertEquals(input, AmountValidation.Invalid(reason), validate(input))
        }
    }

    @Test
    fun wholeAndDecimalAmounts_areValid() {
        assertValid(Money.ofRupees(4201), "4201")
        assertValid(Money(4201_50), "4201.5")
        assertValid(Money(1), "0.01")
        assertValid(Money(250_50), "250.50")
    }

    @Test
    fun missingDigitsAroundThePoint_readAsZero() {
        assertValid(Money.ofRupees(12), "12.")
        assertValid(Money(50), ".5")
    }

    @Test
    fun oneCrore_isTheLargestValidAmount() {
        assertValid(Money.ofRupees(10_000_000), "10000000")
        assertValid(Money.ofRupees(10_000_000), "10000000.00")
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
