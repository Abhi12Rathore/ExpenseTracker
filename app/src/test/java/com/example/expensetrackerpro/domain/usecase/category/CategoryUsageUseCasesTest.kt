package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.CategoryCounts
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CategoryUsageUseCasesTest {

    private val food = category(id = 1, name = "Food")
    private val bills = category(id = 2, name = "Bills").copy(isCustom = false)
    private val travel = category(id = 3, name = "Travel")
    private val gifts = category(id = 4, name = "gifts")
    private val salary = category(id = 5, name = "Salary", type = TransactionType.INCOME)
    private val interest = category(id = 6, name = "Interest", type = TransactionType.INCOME)

    private val usages = listOf(
        CategoryUsage(food, totalCount = 40, periodCount = 12, periodAmount = rupees(3000)),
        CategoryUsage(bills, totalCount = 9, periodCount = 2, periodAmount = rupees(8000)),
        // Used before, but not this month
        CategoryUsage(travel, totalCount = 5),
        CategoryUsage(gifts),
        CategoryUsage(salary, totalCount = 6, periodCount = 1, periodAmount = rupees(90000)),
        CategoryUsage(interest)
    )

    private val section = SectionCategoryUsageUseCase()

    // --- Usage for the month we are in

    @Test
    fun usage_isAskedForTheCurrentMonth_andReturnedAsStored() = runTest {
        val timeProvider = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 10, hour = 15))
        val repository = FakeCategoryRepository(listOf(food, bills))
        repository.setUsage(food.id, totalCount = 40, periodCount = 12, periodAmount = rupees(3000))
        val getCategoryUsage = GetCategoryUsageUseCase(
            repository,
            GetCurrentPeriodUseCase(timeProvider),
            GetDateRangeUseCase(timeProvider)
        )

        val result = getCategoryUsage().first()

        assertEquals(
            DateRange(millisAt(2026, Calendar.OCTOBER, 1), millisAt(2026, Calendar.NOVEMBER, 1)),
            repository.usageRange
        )
        assertEquals(
            listOf(
                CategoryUsage(food, totalCount = 40, periodCount = 12, periodAmount = rupees(3000)),
                CategoryUsage(bills)
            ),
            result
        )
    }

    // --- Sections

    @Test
    fun usedThisMonth_areListedByAmount_highestFirst() {
        val sections = section(usages, TransactionType.EXPENSE, "")

        // Bills has fewer transactions than Food but more money
        assertEquals(listOf(bills, food), sections.mostUsed.map { it.category })
    }

    @Test
    fun notUsedThisMonth_areListedByName_ignoringCase() {
        val sections = section(usages, TransactionType.EXPENSE, "")

        assertEquals(listOf(gifts, travel), sections.unused.map { it.category })
    }

    @Test
    fun equalAmounts_fallBackToTheName() {
        val tied = listOf(
            CategoryUsage(travel, totalCount = 1, periodCount = 1, periodAmount = rupees(500)),
            CategoryUsage(food, totalCount = 1, periodCount = 1, periodAmount = rupees(500))
        )

        assertEquals(listOf(food, travel), section(tied, TransactionType.EXPENSE, "").mostUsed.map { it.category })
    }

    @Test
    fun onlyTheAskedType_isListed_butBothAreCounted() {
        val sections = section(usages, TransactionType.INCOME, "")

        assertEquals(listOf(salary), sections.mostUsed.map { it.category })
        assertEquals(listOf(interest), sections.unused.map { it.category })
        assertEquals(4, sections.expenseCount)
        assertEquals(2, sections.incomeCount)
    }

    @Test
    fun search_matchesFromTheFirstCharacter_ignoringCaseAndSpaces_andLeavesTheCountsAlone() {
        val sections = section(usages, TransactionType.EXPENSE, " F ")

        assertEquals(listOf(food), sections.mostUsed.map { it.category })
        assertEquals(listOf(gifts), sections.unused.map { it.category })
        assertEquals(4, sections.expenseCount)
        assertEquals(2, sections.incomeCount)
    }

    @Test
    fun aSearchThatMatchesNothing_isEmpty() {
        assertTrue(section(usages, TransactionType.EXPENSE, "zzz").isEmpty)
    }

    // --- Counts

    @Test
    fun counts_followTheStoredCategories() = runTest {
        val repository = FakeCategoryRepository(listOf(food, bills, salary))
        val getCategoryCounts = GetCategoryCountsUseCase(repository)

        // Bills is built in
        assertEquals(CategoryCounts(total = 3, custom = 2), getCategoryCounts().first())

        repository.insertCategory(category(id = 0, name = "Pets"))

        assertEquals(CategoryCounts(total = 4, custom = 3), getCategoryCounts().first())
    }
}
