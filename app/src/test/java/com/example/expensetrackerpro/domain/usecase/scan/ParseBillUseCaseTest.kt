package com.example.expensetrackerpro.domain.usecase.scan

import com.example.expensetrackerpro.domain.models.CategoryHint
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionDraft
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.billFixture
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.utcDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/** Whole bills, as text in `src/test/resources/bills`, against everything read from each. */
class ParseBillUseCaseTest {

    // Monday 12 Oct 2026, 12:00 in Kolkata
    private val today = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 12, hour = 12)).now()

    private val parseBill = ParseBillUseCase()

    private fun parse(fixture: String): TransactionDraft = parseBill(billFixture(fixture), today)

    @Test
    fun restaurant_takesTheGrandTotal_notTheSubTotalOrTheTaxes() {
        assertEquals(
            TransactionDraft(
                amount = rupees(1428),
                otherAmounts = listOf(rupees(1360), rupees(690), rupees(380)),
                title = "Cafe Coffee Day",
                dateMillis = utcDay(2026, Calendar.OCTOBER, 11),
                note = "Bill no. 4521",
                categoryHint = CategoryHint.FOOD
            ),
            parse("restaurant")
        )
    }

    @Test
    fun grocery_takesTheTotal_notTheItemCountOrWhatWasSaved() {
        assertEquals(
            TransactionDraft(
                amount = rupees(1152),
                otherAmounts = listOf(rupees(640), rupees(235), rupees(165)),
                title = "Reliance Fresh",
                dateMillis = utcDay(2026, Calendar.OCTOBER, 3),
                note = "Bill no. RF/2026/008812",
                categoryHint = CategoryHint.GROCERIES
            ),
            parse("grocery_you_saved")
        )
    }

    @Test
    fun petrolPump_withNoTotalLabel_takesTheLargestAmount_notThePhoneOrReceiptNumber() {
        assertEquals(
            TransactionDraft(
                amount = rupees(2000),
                otherAmounts = listOf(Money(102_86), rupees(48), Money(19_44)),
                title = "Hp Petrol Pump",
                dateMillis = utcDay(2026, Calendar.OCTOBER, 9),
                note = null,
                categoryHint = CategoryHint.FUEL
            ),
            parse("petrol_pump")
        )
    }

    @Test
    fun pharmacy_takesTheNetAmount() {
        assertEquals(
            TransactionDraft(
                amount = Money(213_46),
                // 650 is the strength of a tablet: a number that can't be told from a price
                otherAmounts = listOf(rupees(650), rupees(145), Money(61_20)),
                title = "Apollo Pharmacy",
                dateMillis = utcDay(2026, Calendar.SEPTEMBER, 28),
                note = "Bill no. AP-55231",
                categoryHint = CategoryHint.HEALTH
            ),
            parse("pharmacy")
        )
    }

    @Test
    fun ecommerceInvoice_skipsTheHeadingForTheTitle_andTheOrderNumberForTheAmount() {
        assertEquals(
            TransactionDraft(
                amount = rupees(1499),
                otherAmounts = listOf(Money(1270_34), rupees(450), Money(228_66)),
                title = "Amazon Seller Services",
                dateMillis = utcDay(2026, Calendar.OCTOBER, 1),
                note = "Bill no. BLR5-184467",
                categoryHint = CategoryHint.SHOPPING
            ),
            parse("ecommerce_invoice")
        )
    }

    @Test
    fun totalOnTheNextLine_isFound_andAnAmbiguousDateIsReadDayFirst() {
        assertEquals(
            TransactionDraft(
                amount = rupees(525),
                otherAmounts = listOf(rupees(500), rupees(360), rupees(140)),
                title = "Sagar Ratna Restaurant",
                dateMillis = utcDay(2026, Calendar.OCTOBER, 5),
                note = "Bill no. 2291",
                categoryHint = CategoryHint.FOOD
            ),
            parse("total_on_next_line")
        )
    }

    @Test
    fun onlyASubTotal_givesNoAmount_butOffersWhatIsOnTheBill() {
        assertEquals(
            TransactionDraft(
                amount = null,
                otherAmounts = listOf(rupees(510), rupees(280), rupees(150)),
                title = "Annapurna Sweets"
            ),
            parse("only_sub_total")
        )
    }

    @Test
    fun noDate_leavesTheDateOut() {
        assertEquals(
            TransactionDraft(
                amount = rupees(105),
                otherAmounts = listOf(rupees(60), rupees(45)),
                title = "Chai Point"
            ),
            parse("no_date")
        )
    }

    @Test
    fun aDateInTheFuture_isLeftOut() {
        assertEquals(
            TransactionDraft(
                amount = rupees(220),
                title = "Blue Tokai Coffee",
                dateMillis = null,
                note = "Bill no. 118",
                categoryHint = CategoryHint.FOOD
            ),
            parse("future_date")
        )
    }

    @Test
    fun aDateThatOnlyReadsMonthFirst_isReadThatWay() {
        assertEquals(
            TransactionDraft(
                amount = rupees(395),
                title = "Starbucks Coffee",
                dateMillis = utcDay(2026, Calendar.SEPTEMBER, 25),
                note = "Bill no. 90412",
                categoryHint = CategoryHint.FOOD
            ),
            parse("month_first_date")
        )
    }

    @Test
    fun rupeesWithSlashDash_areRead() {
        assertEquals(
            TransactionDraft(
                amount = rupees(2030),
                otherAmounts = listOf(rupees(1250), rupees(780)),
                title = "Shree Ganesh Kirana Store",
                dateMillis = utcDay(2026, Calendar.OCTOBER, 10),
                categoryHint = CategoryHint.GROCERIES
            ),
            parse("rupees_slash_dash")
        )
    }

    @Test
    fun amountsWithLakhCommas_areRead_andSavingsAreNotTheTotal() {
        assertEquals(
            TransactionDraft(
                amount = rupees(104_900),
                otherAmounts = listOf(rupees(114_900), rupees(10_000)),
                title = "Croma",
                dateMillis = utcDay(2026, Calendar.SEPTEMBER, 30),
                note = "Bill no. CR-2026-44871"
            ),
            parse("amounts_with_commas")
        )
    }

    @Test
    fun lettersScannedForDigits_arePutRight() {
        assertEquals(
            TransactionDraft(
                amount = rupees(336),
                otherAmounts = listOf(rupees(320), rupees(240), rupees(80)),
                title = "Hotel Saravana Bhavan",
                dateMillis = utcDay(2026, Calendar.OCTOBER, 10),
                note = "Bill no. 3017"
            ),
            parse("ocr_noise")
        )
    }

    @Test
    fun noText_givesAnEmptyDraft() {
        assertEquals(TransactionDraft(), parseBill(billFixtureOf(), today))
    }

    private fun billFixtureOf(vararg lines: String) = com.example.expensetrackerpro.fakes.recognizedText(*lines)
}
