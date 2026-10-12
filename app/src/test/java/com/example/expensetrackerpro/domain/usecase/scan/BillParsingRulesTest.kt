package com.example.expensetrackerpro.domain.usecase.scan

import com.example.expensetrackerpro.domain.models.CategoryHint
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionDraft
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.recognizedText
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.utcDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/** The parsing rules one at a time, on a line or two each. */
class BillParsingRulesTest {

    // Monday 12 Oct 2026, 12:00 in Kolkata
    private val today = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 12, hour = 12)).now()

    private val parseBill = ParseBillUseCase()

    private fun parse(vararg lines: String): TransactionDraft = parseBill(recognizedText(*lines), today)

    // --- Amount

    @Test
    fun amount_isReadHoweverRupeesAreWritten() {
        listOf("Total ₹1,428.00", "Total Rs. 1428/-", "Total Rs.1428", "Total INR 1428", "TOTAL: 1428").forEach {
            assertEquals(it, rupees(1428), parse(it).amount)
        }
        assertEquals(Money(1428_50), parse("Total 1428.5").amount)
    }

    @Test
    fun amount_prefersTheStrongerLabel_whereverItIsPrinted() {
        assertEquals(rupees(500), parse("Grand Total 500", "Total 450", "Net Amount 480").amount)
        assertEquals(rupees(480), parse("Total 450", "Net Payable 480").amount)
        assertEquals(rupees(480), parse("To Pay 480", "Total 450").amount)
        assertEquals(rupees(480), parse("Amount Payable: 480", "Total 450").amount)
    }

    @Test
    fun amount_ofALabelPrintedTwice_isTheLastOne() {
        assertEquals(rupees(472), parse("Total 450.00", "Service charge 22.00", "Total 472.00").amount)
    }

    @Test
    fun amount_skipsTotalsOfSomethingElse() {
        listOf(
            "Sub Total 900", "Sub-Total: 900", "Subtotal 900", "Total GST 900", "Total Tax 900",
            "Total Discount 900", "Total Saved 900", "Total Items 900", "Total Qty 900",
            "Total after round off 900", "CGST on Net Amount 900"
        ).forEach {
            assertNull(it, parse("Pizza 250.00", it).amount)
        }
    }

    @Test
    fun amount_besideAStrongLabel_isTaken_evenWhenTheLineMentionsTax() {
        assertEquals(rupees(1428), parse("Grand Total (incl. GST) 1,428.00").amount)
        assertEquals(rupees(1428), parse("Net Amount incl. of all taxes 1428.00").amount)
    }

    @Test
    fun amount_onTheNextLine_isTakenOnlyWhenThatLineIsJustANumber() {
        assertEquals(rupees(525), parse("Grand Total", "525.00").amount)
        assertEquals(rupees(525), parse("Burger 300.00", "Total", "Cash 600.00", "Net Payable 525.00").amount)
    }

    @Test
    fun amount_withoutAnyTotalLabel_isTheLargest_leavingOutWhatIsNotMoney() {
        val draft = parse(
            "Ph: 98450 12345",
            "+91 98450 12345",
            "GSTIN 29AAACC1234F1Z5",
            "Bill No: 8841  12/10/2026  14:35",
            "Bengaluru 560001",
            "Samosa 2 40.00",
            "Chai 3 45.00",
            "Lottery prize 10,00,000.00"
        )

        assertEquals(rupees(45), draft.amount)
        assertEquals(listOf(rupees(40)), draft.otherAmounts)
    }

    @Test
    fun amount_atOrAboveTenLakh_isNotTaken_evenBesideALabel() {
        assertNull(parse("Grand Total 10,00,000.00").amount)
        assertEquals(Money(9_99_999_99), parse("Grand Total 9,99,999.99").amount)
    }

    @Test
    fun otherAmounts_areAtMostThree_largestFirst_withoutRepeats() {
        val draft = parse("A 10.00", "B 50.00", "C 30.00", "D 50.00", "E 20.00", "Total 110.00")

        assertEquals(rupees(110), draft.amount)
        assertEquals(listOf(rupees(50), rupees(30), rupees(20)), draft.otherAmounts)
    }

    @Test
    fun quantitiesAndPercentages_areNotAmounts() {
        val draft = parse("Milk 500ml 2 x 28.00", "GST 5% 2.80", "Oil 5l 780.00", "Total 838.80")

        assertEquals(listOf(rupees(780), rupees(28), Money(2_80)), draft.otherAmounts)
    }

    // --- Title

    @Test
    fun title_isTheTallestOfTheFirstThreeLines() {
        assertEquals("Udupi Grand", parse("Welcome", "[h=40] UDUPI GRAND", "[h=30] Pure Veg", "[h=90] BIG PRINT").title)
    }

    @Test
    fun title_ofLinesEquallyTall_isTheFirst() {
        assertEquals("Udupi Grand", parse("UDUPI GRAND", "Pure Veg").title)
    }

    @Test
    fun title_isNeverAHeadingAPhoneNumberOrMostlyDigits() {
        assertEquals(
            "Udupi Grand",
            parse("[h=50] TAX INVOICE", "[h=45] Ph: 080 4123 4567", "Udupi Grand", "[h=60] Later line").title
        )
        assertNull(parse("[h=50] BILL", "GSTIN: 29AAACC1234F1Z5", "9845012345").title)
    }

    @Test
    fun title_isTitleCased_withoutDecoration_andCutAtFortyCharacters() {
        assertEquals("Cafe Coffee Day", parse("*** CAFE   COFFEE DAY ***").title)
        assertEquals(
            "Sri Venkateshwara Sweets And Savouries P",
            parse("SRI VENKATESHWARA SWEETS AND SAVOURIES PRIVATE LIMITED").title
        )
    }

    // --- Date

    @Test
    fun date_isReadInEveryFormat() {
        val eleventh = utcDay(2026, Calendar.OCTOBER, 11)

        listOf(
            "11/10/2026", "11-10-26", "11.10.2026", "11 Oct 2026", "11-Oct-26", "11 October 2026",
            "2026-10-11", "Dt: 11/10/2026 13:05", "l1/1O/2026"
        ).forEach {
            assertEquals(it, eleventh, parse(it).dateMillis)
        }
    }

    @Test
    fun date_isKeptFromTodayBackToAYearAgo_andDroppedOutsideThat() {
        assertEquals(utcDay(2026, Calendar.OCTOBER, 12), parse("12/10/2026").dateMillis)
        assertEquals(utcDay(2025, Calendar.OCTOBER, 12), parse("12/10/2025").dateMillis)
        assertNull(parse("13/10/2026").dateMillis)
        assertNull(parse("11/10/2025").dateMillis)
    }

    @Test
    fun date_thatIsNoDay_isNotADate() {
        assertNull(parse("31/02/2026").dateMillis)
        assertNull(parse("14/13/2026").dateMillis)
    }

    @Test
    fun date_isTheFirstOneThatCanBeTrusted() {
        assertEquals(
            utcDay(2026, Calendar.OCTOBER, 2),
            parse("Licence valid till 31/03/2029", "Date: 02/10/2026", "Printed 03/10/2026").dateMillis
        )
    }

    // --- Note

    @Test
    fun note_isTheBillNumber_howeverItIsLabelled() {
        mapOf(
            "Bill No: 4521" to "Bill no. 4521",
            "BILL NO.4521" to "Bill no. 4521",
            "Invoice No - A/22/0045" to "Bill no. A/22/0045",
            "Inv No 77" to "Bill no. 77",
            "Invoice Number: INV-9" to "Bill no. INV-9",
            "Bill # 12" to "Bill no. 12"
        ).forEach { (line, note) -> assertEquals(line, note, parse(line).note) }
    }

    @Test
    fun note_needsANumberAfterTheLabel() {
        assertNull(parse("Bill Notes: none").note)
        assertNull(parse("Bill No: Cash").note)
        assertNull(parse("Table No: 12").note)
    }

    // --- Category hint

    @Test
    fun categoryHint_comesFromTheMerchantsName() {
        mapOf(
            "Sharma Restaurant" to CategoryHint.FOOD,
            "Order from Swiggy" to CategoryHint.FOOD,
            "DMart Avenue" to CategoryHint.GROCERIES,
            "Big Bazaar" to CategoryHint.GROCERIES,
            "IOCL Dealer" to CategoryHint.FUEL,
            "HP Auto Care" to CategoryHint.FUEL,
            "Wellness Chemist" to CategoryHint.HEALTH,
            "City Medical Stores" to CategoryHint.HEALTH
        ).forEach { (line, hint) -> assertEquals(line, hint, parse(line).categoryHint) }
    }

    @Test
    fun categoryHint_needsAShortKeywordToStandAlone_andIsNullWithoutOne() {
        // "hp" inside a word is not the fuel company
        assertNull(parse("Alphpa Traders").categoryHint)
        assertNull(parse("Sharma Stationers").categoryHint)
    }
}
