package com.example.expensetrackerpro.data.mapper

import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.model.TransactionWithCategory
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.rupees
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionMapperTest {

    private val entity = TransactionEntity(
        id = 7,
        title = "Lunch",
        amountPaise = 250_50,
        note = "Team lunch",
        dateMillis = 1_700_000_000_000,
        categoryId = 3,
        transactionType = TransactionType.EXPENSE
    )

    private val domain = Transaction(
        id = 7,
        title = "Lunch",
        amount = Money(250_50),
        note = "Team lunch",
        dateMillis = 1_700_000_000_000,
        categoryId = 3,
        categoryName = "Food",
        transactionType = TransactionType.EXPENSE,
        categoryIcon = CategoryIcon.RESTAURANT
    )

    // As a read returns it: the stored row plus its category's name and icon
    private fun TransactionEntity.read() = TransactionWithCategory(this, "Food", CategoryIcon.RESTAURANT)

    @Test
    fun readToDomain_copiesEveryField_andTakesTheNameAndIconFromTheCategory() {
        assertEquals(domain, entity.read().toDomain())
    }

    @Test
    fun domainToEntity_copiesEveryStoredField() {
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun noteThatOnlyRepeatsTheTitle_isReadAsNoNote() {
        val legacy = entity.copy(note = "Lunch")

        assertEquals("", legacy.read().toDomain().note)
        assertEquals("Lunch", legacy.read().toDomain().title)
    }

    @Test
    fun emptyNote_staysEmpty() {
        assertEquals("", entity.copy(note = "").read().toDomain().note)
    }

    @Test
    fun roundTrip_preservesIncomeType() {
        val income = entity.copy(transactionType = TransactionType.INCOME)

        assertEquals(income, income.read().toDomain().toEntity())
    }

    @Test
    fun newTransaction_keepsZeroIdSoRoomAutoGenerates() {
        val unsaved = Transaction(
            title = "Salary",
            amount = rupees(1000),
            note = "Salary",
            dateMillis = 0L,
            categoryId = 1,
            categoryName = "Salary",
            transactionType = TransactionType.INCOME
        )

        assertEquals(0, unsaved.toEntity().id)
    }
}
