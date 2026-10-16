package com.example.expensetrackerpro.domain.usecase.quickadd

import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.transaction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/** Which repeats the Add form offers in one tap, and the usual amount each one shows. */
class GetFrequentTransactionsUseCaseTest {

    // Thursday 8 Oct 2026, 12:00 in Kolkata
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)
    private var nextId = 1

    private fun spend(title: String, amount: Money = rupees(100), daysAgo: Int = 1, categoryId: Int = 1) =
        transaction(id = nextId++, amount = amount, dateMillis = now - daysAgo * DAY_MILLIS, title = title)
            .copy(categoryId = CategoryId(categoryId))

    private suspend fun frequent(vararg spends: com.example.expensetrackerpro.domain.models.Transaction) =
        GetFrequentTransactionsUseCase(FakeTransactionRepository(spends.toList()), FakeTimeProvider(now))(TransactionType.EXPENSE)

    @Test
    fun noRepeats_noneOffered() = runTest {
        assertEquals(emptyList<Any>(), frequent(spend("Swiggy"), spend("Swiggy"), spend("Uber")))
    }

    @Test
    fun theUsualAmount_isTheMiddleOne() = runTest {
        val found = frequent(spend("Swiggy", rupees(400)), spend("Swiggy", rupees(2_000)), spend("Swiggy", rupees(450)))

        assertEquals(rupees(450), found.single().usualAmount)
        assertEquals(3, found.single().count)
    }

    @Test
    fun anEvenCount_takesHalfwayBetweenTheMiddleTwo_toTheNearestMinorUnit() = runTest {
        val found = frequent(
            spend("Chai", Money(10_00)),
            spend("Chai", Money(12_01)),
            spend("Chai", Money(15_00)),
            spend("Chai", Money(90_00))
        )

        // (12.01 + 15.00) / 2 = 13.505, halves rounded up
        assertEquals(Money(13_51), found.single().usualAmount)
    }

    @Test
    fun mostFrequentFirst_thenTheOneEnteredLast_atMostThree() = runTest {
        val spends = buildList {
            repeat(3) { add(spend("Rent", daysAgo = 30 + it)) }
            repeat(5) { add(spend("Swiggy", daysAgo = 2 + it)) }
            repeat(3) { add(spend("Petrol", daysAgo = 1 + it)) }
            repeat(4) { add(spend("Uber", daysAgo = 10 + it)) }
        }

        assertEquals(listOf("Swiggy", "Uber", "Petrol"), frequent(*spends.toTypedArray()).map { it.title })
    }

    @Test
    fun theTitle_isAsWrittenLast() = runTest {
        val found = frequent(spend("swiggy", daysAgo = 3), spend("SWIGGY", daysAgo = 2), spend("Swiggy ", daysAgo = 1))

        assertEquals("Swiggy", found.single().title)
    }

    @Test
    fun oneTitleInTwoCategories_isTwoRepeats() = runTest {
        val spends = buildList {
            repeat(3) { add(spend("Amazon", categoryId = 1)) }
            repeat(3) { add(spend("Amazon", categoryId = 2)) }
        }

        assertEquals(listOf(CategoryId(1), CategoryId(2)), frequent(*spends.toTypedArray()).map { it.categoryId }.sortedBy { it.value })
    }

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
