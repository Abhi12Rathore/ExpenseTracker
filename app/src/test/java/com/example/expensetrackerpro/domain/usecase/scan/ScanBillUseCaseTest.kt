package com.example.expensetrackerpro.domain.usecase.scan

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.getOrNull
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryHint
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.FakeBillScannerAvailability
import com.example.expensetrackerpro.fakes.FakeBillTextReader
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.billFixture
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.recognizedText
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.utcDay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScanBillUseCaseTest {

    private val reader = FakeBillTextReader()
    private val timeProvider = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 12, hour = 12))
    private val scanBill = ScanBillUseCase(reader, ParseBillUseCase(), timeProvider)

    private val image = DocumentUri("file:///cache/bill.jpg")

    @Test
    fun aReadableBill_isReadThenParsed() = runTest {
        reader.result = Outcome.Success(billFixture("restaurant"))

        val draft = scanBill(image).getOrNull()

        assertEquals(listOf(image), reader.read)
        assertEquals(rupees(1428), draft?.amount)
        assertEquals("Cafe Coffee Day", draft?.title)
    }

    @Test
    fun theDate_isCheckedAgainstTheClock() = runTest {
        reader.result = Outcome.Success(recognizedText("Total 100.00", "Date: 11/10/2026"))

        assertEquals(utcDay(2026, Calendar.OCTOBER, 11), scanBill(image).getOrNull()?.dateMillis)

        // The day before the bill's date, the same bill is dated in the future
        timeProvider.nowMillis = millisAt(2026, Calendar.OCTOBER, 10, hour = 12)
        assertNull(scanBill(image).getOrNull()?.dateMillis)
    }

    @Test
    fun aPhotoThatCouldNotBeRead_fails() = runTest {
        reader.result = Outcome.Failure(DomainError.BillUnreadable)

        assertEquals(Outcome.Failure(DomainError.BillUnreadable), scanBill(image))
    }

    @Test
    fun textWithNoAmountAtAll_isNotABill() = runTest {
        reader.result = Outcome.Success(recognizedText("[h=40] HAPPY BIRTHDAY", "With love from all of us"))
        assertEquals(Outcome.Failure(DomainError.BillUnreadable), scanBill(image))

        reader.result = Outcome.Success(recognizedText())
        assertEquals(Outcome.Failure(DomainError.BillUnreadable), scanBill(image))
    }

    @Test
    fun aBillWithoutATotal_isStillUsable() = runTest {
        reader.result = Outcome.Success(billFixture("only_sub_total"))

        val draft = scanBill(image).getOrNull()

        assertNull(draft?.amount)
        assertEquals(listOf(rupees(510), rupees(280), rupees(150)), draft?.otherAmounts)
    }

    // --- The small use cases beside it

    @Test
    fun scannerAvailability_isReadFromTheDevice() {
        val availability = FakeBillScannerAvailability(available = true)
        val isAvailable = IsBillScannerAvailableUseCase(availability)

        assertTrue(isAvailable())
        availability.available = false
        assertFalse(isAvailable())
    }

    @Test
    fun aHint_resolvesToItsFirstCategoryThatIsThere() {
        val resolve = ResolveCategoryHintUseCase()
        val transport = category(id = 1, name = "Transport")
        val fuel = category(id = 2, name = "fuel")
        val salary = category(id = 3, name = "Salary", type = TransactionType.INCOME)

        assertEquals(fuel, resolve(CategoryHint.FUEL, listOf(transport, fuel)))
        assertEquals(transport, resolve(CategoryHint.FUEL, listOf(transport)))
        assertNull(resolve(CategoryHint.FUEL, listOf(salary)))
        assertNull(resolve(CategoryHint.FOOD, emptyList<Category>()))
    }
}
