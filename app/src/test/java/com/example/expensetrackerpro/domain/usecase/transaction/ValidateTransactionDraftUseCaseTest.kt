package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.AmountError
import com.example.expensetrackerpro.domain.models.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateTransactionDraftUseCaseTest {

    private val validate = ValidateTransactionDraftUseCase(ValidateAmountUseCase())

    @Test
    fun amountTitleAndCategory_isValid_andNoteIsOptional() {
        val result = validate(amount = "250.50", title = "Lunch", note = "", hasCategory = true)

        assertTrue(result.isValid)
        assertEquals(Money(250_50), result.amount)
    }

    @Test
    fun missingCategory_isInvalid() {
        val result = validate(amount = "250", title = "Lunch", note = "", hasCategory = false)

        assertTrue(result.isCategoryMissing)
        assertFalse(result.isValid)
    }

    @Test
    fun blankTitle_isInvalid_evenWithANote() {
        listOf("", "   ").forEach { title ->
            val result = validate(amount = "250", title = title, note = "From the note", hasCategory = true)

            assertTrue(result.isTitleBlank)
            assertFalse(result.isValid)
        }
    }

    @Test
    fun invalidAmount_carriesTheReasonAndNoAmount() {
        val result = validate(amount = "1e5", title = "Lunch", note = "", hasCategory = true)

        assertEquals(AmountError.MALFORMED, result.amountError)
        assertNull(result.amount)
        assertFalse(result.isValid)
    }

    @Test
    fun note_mayBeExactlyTwoHundredCharacters() {
        val atLimit = validate(amount = "250", title = "Lunch", note = "n".repeat(200), hasCategory = true)
        val overLimit = validate(amount = "250", title = "Lunch", note = "n".repeat(201), hasCategory = true)

        assertTrue(atLimit.isValid)
        assertTrue(overLimit.isNoteTooLong)
        assertFalse(overLimit.isValid)
    }
}
