package com.example.expensetrackerpro.domain.usecase.transaction

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateTransactionInputUseCaseTest {

    private val validate = ValidateTransactionInputUseCase()

    @Test
    fun categoryNoteAndPositiveAmount_isValid() {
        assertTrue(validate(amount = "250.50", note = "Lunch", hasCategory = true))
    }

    @Test
    fun missingCategory_isInvalid() {
        assertFalse(validate(amount = "250", note = "Lunch", hasCategory = false))
    }

    @Test
    fun blankNote_isInvalid() {
        assertFalse(validate(amount = "250", note = "", hasCategory = true))
        assertFalse(validate(amount = "250", note = "   ", hasCategory = true))
    }

    @Test
    fun zeroOrNegativeAmount_isInvalid() {
        assertFalse(validate(amount = "0", note = "Lunch", hasCategory = true))
        assertFalse(validate(amount = "-5", note = "Lunch", hasCategory = true))
    }

    @Test
    fun emptyOrNonNumericAmount_isInvalid() {
        assertFalse(validate(amount = "", note = "Lunch", hasCategory = true))
        assertFalse(validate(amount = "abc", note = "Lunch", hasCategory = true))
    }
}
