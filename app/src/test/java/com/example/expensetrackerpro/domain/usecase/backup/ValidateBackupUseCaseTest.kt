package com.example.expensetrackerpro.domain.usecase.backup

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.getOrNull
import com.example.expensetrackerpro.domain.models.Backup
import com.example.expensetrackerpro.domain.models.BackupDocument
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionId
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.backupDocument
import com.example.expensetrackerpro.fakes.categoryRecord
import com.example.expensetrackerpro.fakes.transactionRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateBackupUseCaseTest {

    private val validate = ValidateBackupUseCase()

    private val invalid = Outcome.Failure(DomainError.BackupInvalid)

    private fun withCategory(
        id: Int = 1,
        name: String = "Food",
        type: String = "EXPENSE",
        icon: String = "restaurant"
    ): BackupDocument = backupDocument(
        categories = listOf(categoryRecord(id = id, name = name, type = type, icon = icon)),
        transactions = emptyList()
    )

    private fun withTransaction(
        amountPaise: Long = 250_00,
        categoryId: Int = 1,
        type: String = "EXPENSE",
        title: String = "Lunch"
    ): BackupDocument = backupDocument(
        categories = listOf(categoryRecord(id = 1), categoryRecord(id = 2, name = "Salary", type = "INCOME")),
        transactions = listOf(
            transactionRecord(title = title, amountPaise = amountPaise, categoryId = categoryId, type = type)
        )
    )

    @Test
    fun aSoundFile_becomesABackup_withEveryValueTyped() {
        val food = Category(CategoryId(1), "Food", TransactionType.EXPENSE, CategoryIcon.RESTAURANT, isCustom = false)
        val salary = Category(CategoryId(2), "Salary", TransactionType.INCOME, CategoryIcon.PAYMENTS, isCustom = false)
        val pottery = Category(CategoryId(7), "Pottery", TransactionType.EXPENSE, CategoryIcon.PETS, isCustom = true)

        val expected = Backup(
            createdAtMillis = 1_000L,
            appVersion = "1.0",
            categories = listOf(food, salary, pottery),
            transactions = listOf(
                Transaction(
                    title = "Lunch", amount = Money(250_00), note = "", dateMillis = 300L,
                    categoryId = food.id, categoryName = "Food",
                    transactionType = TransactionType.EXPENSE, categoryIcon = CategoryIcon.RESTAURANT
                ),
                Transaction(
                    title = "Pay", amount = Money(85_000_00), note = "", dateMillis = 100L,
                    categoryId = salary.id, categoryName = "Salary",
                    transactionType = TransactionType.INCOME, categoryIcon = CategoryIcon.PAYMENTS
                ),
                Transaction(
                    title = "Clay", amount = Money(5), note = "", dateMillis = 200L,
                    categoryId = pottery.id, categoryName = "Pottery",
                    transactionType = TransactionType.EXPENSE, categoryIcon = CategoryIcon.PETS
                )
            )
        )

        assertEquals(Outcome.Success(expected), validate(backupDocument()))
    }

    @Test
    fun restoredTransactions_haveNoIdOfTheirOwn() {
        val backup = validate(backupDocument()).getOrNull()

        assertTrue(backup?.transactions.orEmpty().all { it.id == TransactionId.UNSAVED })
    }

    @Test
    fun aFileWithNothingInIt_isStillABackup() {
        val result = validate(backupDocument(categories = emptyList(), transactions = emptyList()))

        assertEquals(emptyList<Category>(), result.getOrNull()?.categories)
        assertEquals(emptyList<Transaction>(), result.getOrNull()?.transactions)
    }

    @Test
    fun anotherFormat_isInvalid() {
        assertEquals(invalid, validate(backupDocument(format = "someone-elses-backup")))
        assertEquals(invalid, validate(backupDocument(format = "")))
    }

    @Test
    fun aNewerVersion_isTooNew() {
        assertEquals(
            Outcome.Failure(DomainError.BackupTooNew),
            validate(backupDocument(version = Backup.VERSION + 1))
        )
    }

    @Test
    fun aNewerVersionOfAnotherFormat_isJustInvalid() {
        assertEquals(invalid, validate(backupDocument(format = "other", version = Backup.VERSION + 1)))
    }

    @Test
    fun aVersionBeforeTheFirst_isInvalid() {
        assertEquals(invalid, validate(backupDocument(version = 0)))
        assertEquals(invalid, validate(backupDocument(version = -1)))
    }

    @Test
    fun anUnknownCategoryType_isInvalid() {
        assertEquals(invalid, validate(withCategory(type = "TRANSFER")))
        // Stored by name, exactly
        assertEquals(invalid, validate(withCategory(type = "expense")))
    }

    @Test
    fun anUnknownIcon_isInvalid() {
        assertEquals(invalid, validate(withCategory(icon = "rocket")))
        // Icons are stored by key, not by the enum entry's name
        assertEquals(invalid, validate(withCategory(icon = "RESTAURANT")))
    }

    @Test
    fun everyIconTheAppHas_isKnown() {
        CategoryIcon.entries.forEach { icon ->
            assertEquals(icon, validate(withCategory(icon = icon.key)).getOrNull()?.categories?.single()?.icon)
        }
    }

    @Test
    fun aBlankCategoryName_isInvalid() {
        assertEquals(invalid, validate(withCategory(name = "")))
        assertEquals(invalid, validate(withCategory(name = "   ")))
    }

    @Test
    fun aCategoryIdTheDatabaseWouldReplace_isInvalid() {
        assertEquals(invalid, validate(withCategory(id = 0)))
        assertEquals(invalid, validate(withCategory(id = -3)))
    }

    @Test
    fun twoCategoriesWithTheSameId_areInvalid() {
        val document = backupDocument(
            categories = listOf(categoryRecord(id = 4, name = "Food"), categoryRecord(id = 4, name = "Fuel")),
            transactions = emptyList()
        )

        assertEquals(invalid, validate(document))
    }

    @Test
    fun twoCategoriesWithTheSameName_areFine() {
        val document = backupDocument(
            categories = listOf(categoryRecord(id = 4, name = "Other"), categoryRecord(id = 5, name = "Other")),
            transactions = emptyList()
        )

        assertEquals(2, validate(document).getOrNull()?.categories?.size)
    }

    @Test
    fun anAmountThatIsNotPositive_isInvalid() {
        assertEquals(invalid, validate(withTransaction(amountPaise = 0)))
        assertEquals(invalid, validate(withTransaction(amountPaise = -250_00)))
    }

    @Test
    fun theSmallestAmount_isValid() {
        assertEquals(Money(1), validate(withTransaction(amountPaise = 1)).getOrNull()?.transactions?.single()?.amount)
    }

    @Test
    fun aTransactionWhoseCategoryIsNotInTheFile_isInvalid() {
        assertEquals(invalid, validate(withTransaction(categoryId = 99)))
    }

    @Test
    fun aTransactionOfAnotherTypeThanItsCategory_isInvalid() {
        // Category 1 is an expense category, category 2 an income one
        assertEquals(invalid, validate(withTransaction(categoryId = 1, type = "INCOME")))
        assertEquals(invalid, validate(withTransaction(categoryId = 2, type = "EXPENSE")))
    }

    @Test
    fun anUnknownTransactionType_isInvalid() {
        assertEquals(invalid, validate(withTransaction(type = "REFUND")))
    }

    @Test
    fun oneBadTransactionAmongGoodOnes_turnsTheWholeFileDown() {
        val document = backupDocument()
        val withOneBad = document.copy(transactions = document.transactions + transactionRecord(categoryId = 99))

        assertEquals(invalid, validate(withOneBad))
    }

    @Test
    fun aBlankTitleOrNote_isKeptAsItIs() {
        val backup = validate(withTransaction(title = "")).getOrNull()

        assertEquals("", backup?.transactions?.single()?.title)
    }
}
