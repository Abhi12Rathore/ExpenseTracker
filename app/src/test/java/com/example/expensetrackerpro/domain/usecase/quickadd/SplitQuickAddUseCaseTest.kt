package com.example.expensetrackerpro.domain.usecase.quickadd

import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.models.FieldConfidence
import com.example.expensetrackerpro.domain.models.QuickAddBatch
import com.example.expensetrackerpro.domain.models.QuickAddField
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.parseQuickAdd
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.utcDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/** Several transactions said in one sentence, and the sentences that stay one transaction. */
class SplitQuickAddUseCaseTest {

    // Thursday 8 Oct 2026, 12:00 in Kolkata
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)
    private val split = SplitQuickAddUseCase(parseQuickAdd(FakeTimeProvider(now)))

    private val food = builtIn(1, "Food & Dining", BuiltInCategory.FOOD_DINING)
    private val transport = builtIn(2, "Transport", BuiltInCategory.TRANSPORT)
    private val groceries = builtIn(3, "Groceries", BuiltInCategory.GROCERIES)
    private val salary = builtIn(4, "Salary", BuiltInCategory.SALARY)
    private val categories = listOf(food, transport, groceries, salary)

    private fun builtIn(id: Int, name: String, key: BuiltInCategory) =
        category(id = id, name = name, type = key.type).copy(builtInKey = key)

    private fun split(text: String): QuickAddBatch? = split(text, TransactionType.EXPENSE, categories, emptyList())

    private fun QuickAddBatch?.texts(): List<String>? = this?.parts?.map { it.text }

    private fun QuickAddBatch?.amounts(): List<Long>? = this?.parts?.map { (it.result.draft.amount?.minorUnits ?: 0) / 100 }

    @Test
    fun and_splitsTwoTransactions() {
        val batch = split("450 swiggy and 120 uber")

        assertEquals(listOf("450 swiggy", "120 uber"), batch.texts())
        assertEquals(listOf(450L, 120L), batch.amounts())
        assertEquals(listOf("Swiggy", "Uber"), batch?.parts?.map { it.result.draft.title })
        assertEquals(listOf(food.id, transport.id), batch?.parts?.map { it.result.draft.categoryId })
    }

    @Test
    fun hinglishAur_splits() {
        assertEquals(listOf(450L, 120L), split("450 swiggy aur 120 auto").amounts())
    }

    @Test
    fun commasAndNewLines_split() {
        assertEquals(listOf(450L, 120L, 80L), split("450 swiggy, 120 uber\n80 milk").amounts())
    }

    @Test
    fun theSplitWord_isFoundWholeAndInAnyCase() {
        // "And" splits; "sandwich" and "Anderson" hold the letters but are words of their own
        assertEquals(listOf("90 sandwich", "120 uber"), split("90 sandwich And 120 uber").texts())
        assertNull(split("450 anderson cafe"))
    }

    @Test
    fun aCommaInsideANumber_doesNotSplit() {
        assertEquals(listOf(1_240L, 120L), split("1,240 dmart and 120 uber").amounts())
        assertNull(split("1,05,000 salary"))
    }

    @Test
    fun aPartWithoutItsOwnAmount_meansOneTransaction() {
        assertNull(split("450 chips and coke"))
        // A comma before the note is not a second transaction either
        assertNull(split("450 swiggy, dinner with Rahul"))
    }

    @Test
    fun andBetweenWrittenOutNumbers_isPartOfTheNumber() {
        assertNull(split("one hundred and fifty swiggy"))
    }

    @Test
    fun oneSentence_isNotABatch() {
        assertNull(split("450 swiggy yesterday"))
        assertNull(split(""))
    }

    @Test
    fun atMostFiveParts() {
        assertNotNull(split("1 a, 2 b, 3 c, 4 d, 5 e"))
        assertNull(split("1 a, 2 b, 3 c, 4 d, 5 e, 6 f"))
    }

    @Test
    fun aDateSaidOnce_isEveryParts() {
        val batch = split("450 swiggy and 120 uber yesterday")
        val yesterday = utcDay(2026, Calendar.OCTOBER, 7)

        assertEquals(listOf(yesterday, yesterday), batch?.parts?.map { it.result.draft.dateMillis })
        assertEquals(
            listOf(FieldConfidence.SURE, FieldConfidence.SURE),
            batch?.parts?.map { it.result.confidenceOf(QuickAddField.DATE) }
        )
    }

    @Test
    fun partsThatSayTheirOwnDates_keepThem() {
        val batch = split("450 swiggy yesterday and 120 uber today")

        assertEquals(
            listOf(utcDay(2026, Calendar.OCTOBER, 7), utcDay(2026, Calendar.OCTOBER, 8)),
            batch?.parts?.map { it.result.draft.dateMillis }
        )
    }

    @Test
    fun noDateSaid_leavesEveryPartToday() {
        assertEquals(listOf(null, null), split("450 swiggy and 120 uber")?.parts?.map { it.result.draft.dateMillis })
    }

    @Test
    fun eachPart_isCheckedLikeASentence() {
        // An amount over the limit is left out, so that part has none and the sentence isn't split
        assertNull(split("450 swiggy and 50 lakh car"))
        assertEquals(rupees(450), split("450 swiggy and 50000 salary")?.parts?.first()?.result?.draft?.amount)
    }

    @Test
    fun aPartCanSayItsOwnType() {
        val batch = split("450 swiggy and 50000 salary")

        assertEquals(listOf(null, TransactionType.INCOME), batch?.parts?.map { it.result.draft.type })
        assertEquals(salary.id, batch?.parts?.last()?.result?.draft?.categoryId)
    }
}
