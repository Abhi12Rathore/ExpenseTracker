package com.example.expensetrackerpro.data.mapper

import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionMapperTest {

    private val entity = TransactionEntity(
        id = 7,
        title = "Lunch",
        amount = 250.5,
        note = "Team lunch",
        dateMillis = 1_700_000_000_000,
        categoryId = 3,
        categoryName = "Food",
        transactionType = TransactionType.EXPENSE
    )

    private val domain = Transaction(
        id = 7,
        title = "Lunch",
        amount = 250.5,
        note = "Team lunch",
        dateMillis = 1_700_000_000_000,
        categoryId = 3,
        categoryName = "Food",
        transactionType = TransactionType.EXPENSE
    )

    @Test
    fun entityToDomain_copiesEveryField() {
        assertEquals(domain, entity.toDomain())
    }

    @Test
    fun domainToEntity_copiesEveryField() {
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun noteThatOnlyRepeatsTheTitle_isReadAsNoNote() {
        val legacy = entity.copy(note = "Lunch")

        assertEquals("", legacy.toDomain().note)
        assertEquals("Lunch", legacy.toDomain().title)
    }

    @Test
    fun emptyNote_staysEmpty() {
        assertEquals("", entity.copy(note = "").toDomain().note)
    }

    @Test
    fun roundTrip_preservesIncomeType() {
        val income = entity.copy(transactionType = TransactionType.INCOME)

        assertEquals(income, income.toDomain().toEntity())
    }

    @Test
    fun newTransaction_keepsZeroIdSoRoomAutoGenerates() {
        val unsaved = Transaction(
            title = "Salary",
            amount = 1000.0,
            note = "Salary",
            dateMillis = 0L,
            categoryId = 1,
            categoryName = "Salary",
            transactionType = TransactionType.INCOME
        )

        assertEquals(0, unsaved.toEntity().id)
    }
}
