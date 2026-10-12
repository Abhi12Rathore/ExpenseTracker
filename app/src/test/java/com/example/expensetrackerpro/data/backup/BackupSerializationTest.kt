package com.example.expensetrackerpro.data.backup

import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.model.StoredData
import com.example.expensetrackerpro.data.mapper.toEntity
import com.example.expensetrackerpro.domain.error.getOrNull
import com.example.expensetrackerpro.domain.models.Backup
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.backup.ValidateBackupUseCase
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupSerializationTest {

    private val food = CategoryEntity(id = 1, name = "Food & Dining", type = TransactionType.EXPENSE, icon = CategoryIcon.RESTAURANT, isCustom = false)
    private val salary = CategoryEntity(id = 2, name = "Salary", type = TransactionType.INCOME, icon = CategoryIcon.PAYMENTS, isCustom = false)
    private val custom = CategoryEntity(id = 31, name = "किराना \"local\" 😀", type = TransactionType.EXPENSE, icon = CategoryIcon.SHOPPING_BASKET, isCustom = true)

    private val stored = StoredData(
        categories = listOf(food, salary, custom),
        transactions = listOf(
            TransactionEntity(id = 12, title = "Lunch, \"the good one\"", amountPaise = 250_50, note = "line one\nline two\ttabbed \\ slashed", dateMillis = 1_760_000_000_000L, categoryId = 1, transactionType = TransactionType.EXPENSE),
            TransactionEntity(id = 13, title = "October salary", amountPaise = 1_05_500_00, note = "", dateMillis = 1_759_000_000_000L, categoryId = 2, transactionType = TransactionType.INCOME),
            TransactionEntity(id = 14, title = "चाय ☕ और समोसा", amountPaise = 1, note = "₹20 tip 😀", dateMillis = -5L, categoryId = 31, transactionType = TransactionType.EXPENSE)
        )
    )

    private val dto = stored.toBackupDto(createdAtMillis = 1_760_100_000_000L, appVersion = "1.0")

    private fun encode(backup: BackupDto): String = BackupJson.encodeToString(backup)

    private fun decode(text: String): BackupDto = BackupJson.decodeFromString<BackupDto>(text)

    @Test
    fun aBackup_writtenAndReadBack_isEqual() {
        assertEquals(dto, decode(encode(dto)))
    }

    @Test
    fun aBackup_writtenAndReadBack_restoresWhatWasStored() {
        val backup = ValidateBackupUseCase()(decode(encode(dto)).toDocument()).getOrNull()

        assertEquals(1_760_100_000_000L, backup?.createdAtMillis)
        assertEquals("1.0", backup?.appVersion)
        assertEquals(stored.categories, backup?.categories?.map { it.toEntity() })
        // A transaction's own id is not part of the file
        assertEquals(stored.transactions.map { it.copy(id = 0) }, backup?.transactions?.map { it.toEntity() })
    }

    @Test
    fun theFile_hasTheDocumentedFields() {
        val file = BackupJson.parseToJsonElement(encode(dto)).jsonObject

        assertEquals(
            setOf("format", "version", "createdAt", "appVersion", "categories", "transactions"),
            file.keys
        )
        assertEquals("\"${Backup.FORMAT}\"", file["format"].toString())
        assertEquals(Backup.VERSION.toString(), file["version"].toString())
        assertEquals("1760100000000", file["createdAt"].toString())
        assertEquals(
            setOf("id", "name", "type", "icon", "isCustom"),
            (file.getValue("categories").jsonArray.first() as JsonObject).keys
        )
        assertEquals(
            setOf("title", "amountPaise", "note", "dateMillis", "categoryId", "type"),
            (file.getValue("transactions").jsonArray.first() as JsonObject).keys
        )
    }

    @Test
    fun typeIsWrittenByName_andIconByKey() {
        val category = BackupJson.parseToJsonElement(encode(dto)).jsonObject
            .getValue("categories").jsonArray.first().jsonObject

        assertEquals("\"EXPENSE\"", category["type"].toString())
        assertEquals("\"restaurant\"", category["icon"].toString())
    }

    @Test
    fun moneyIsWrittenAsWholePaise() {
        val transaction = BackupJson.parseToJsonElement(encode(dto)).jsonObject
            .getValue("transactions").jsonArray[1].jsonObject

        assertEquals("10550000", transaction["amountPaise"].toString())
    }

    @Test
    fun fieldsThisVersionDoesNotKnow_areIgnored() {
        val withExtras = encode(dto)
            .replaceFirst("{", "{\"device\":\"Pixel\",")
            .replace("\"isCustom\":", "\"colour\":\"red\",\"isCustom\":")

        assertEquals(dto, decode(withExtras))
    }

    @Test
    fun aMissingField_isNotABackup() {
        val withoutCategories = """{"format":"${Backup.FORMAT}","version":1,"createdAt":1,"appVersion":"1.0","transactions":[]}"""
        val withoutAmount = encode(dto).replaceFirst("\"amountPaise\":25050,", "")

        assertThrows(SerializationException::class.java) { decode(withoutCategories) }
        assertThrows(SerializationException::class.java) { decode(withoutAmount) }
    }

    @Test
    fun aValueOfTheWrongKind_isNotABackup() {
        val textAmount = encode(dto).replaceFirst("\"amountPaise\":25050", "\"amountPaise\":\"lots\"")
        val fractionalAmount = encode(dto).replaceFirst("\"amountPaise\":25050", "\"amountPaise\":250.5")
        val nullName = encode(dto).replaceFirst("\"name\":\"Salary\"", "\"name\":null")

        assertThrows(SerializationException::class.java) { decode(textAmount) }
        assertThrows(SerializationException::class.java) { decode(fractionalAmount) }
        assertThrows(SerializationException::class.java) { decode(nullName) }
    }

    @Test
    fun textThatIsNotJson_isNotABackup() {
        listOf("", "   ", "Date,Type,Category,Title,Note,Amount\r\n2026-10-11,Expense,Food,Lunch,,100.00\r\n", "[1,2,3]", "\"backup\"", "{", "\u0089PNG\r\n\u001a\n\u0000\u0000")
            .forEach { text ->
                assertThrows("for <$text>", SerializationException::class.java) { decode(text) }
            }
    }
}
