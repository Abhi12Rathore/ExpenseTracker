package com.example.expensetrackerpro.domain.usecase.export

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.fakes.FakeCurrencyProvider
import com.example.expensetrackerpro.fakes.dateFormatter
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.transaction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class BuildTransactionsCsvUseCaseTest {

    private val originalLocale = Locale.getDefault()
    private val originalTimeZone = TimeZone.getDefault()

    private val currency = FakeCurrencyProvider()
    private val buildCsv = BuildTransactionsCsvUseCase(dateFormatter(), currency)

    private val header = "Date,Type,Category,Title,Note,Amount,Currency"
    private val oct11 = millisAt(2026, Calendar.OCTOBER, 11, hour = 9)

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    private fun row(
        title: String = "Lunch",
        note: String = "",
        category: String = "Food",
        amount: Money = rupees(100),
        type: TransactionType = TransactionType.EXPENSE,
        dateMillis: Long = oct11
    ): Transaction = transaction(amount = amount, type = type, dateMillis = dateMillis, note = note, title = title)
        .copy(categoryName = category)

    /**
     * The one data line of a file built from [transaction], without its line ending or its last
     * cell, the currency's code, which is checked here and has tests of its own.
     */
    private fun lineFor(transaction: Transaction): String {
        val csv = buildCsv(listOf(transaction))
        assertTrue(csv.startsWith("$header\r\n"))
        assertTrue(csv.endsWith(",${currency.currency.code}\r\n"))

        return csv.removePrefix("$header\r\n").removeSuffix(",${currency.currency.code}\r\n")
    }

    @Test
    fun everyLine_endsWithTheCodeOfTheCurrencyAmountsAreShownIn() {
        val salary = row(title = "Salary", category = "Salary", amount = rupees(105500), dateMillis = millisAt(2026, Calendar.OCTOBER, 1))

        assertEquals(
            "$header\r\n2026-10-01,Expense,Salary,Salary,,105500.00,INR\r\n",
            buildCsv(listOf(salary))
        )

        currency.currency = AppCurrency.USD
        assertTrue(buildCsv(listOf(salary)).endsWith(",105500.00,USD\r\n"))
    }

    @Test
    fun yen_keepBothDecimals_asTheFileHoldsWhatIsStored() {
        currency.currency = AppCurrency.JPY

        assertTrue(buildCsv(listOf(row(amount = Money(450_50)))).endsWith(",450.50,JPY\r\n"))
    }

    @Test
    fun noTransactions_givesOnlyTheHeader() {
        assertEquals("$header\r\n", buildCsv(emptyList()))
    }

    @Test
    fun aTransaction_isOneLineOfSixCells() {
        val salary = row(
            title = "October salary",
            category = "Salary",
            amount = rupees(105500),
            type = TransactionType.INCOME,
            dateMillis = millisAt(2026, Calendar.OCTOBER, 1)
        )

        assertEquals("2026-10-01,Income,Salary,October salary,,105500.00", lineFor(salary))
    }

    @Test
    fun everyLine_endsInCrLf_andNoBareLineFeedIsAdded() {
        val csv = buildCsv(listOf(row(), row(dateMillis = oct11 - 1)))

        assertEquals(3, csv.split("\r\n").count { it.isNotEmpty() })
        assertEquals(csv.count { it == '\n' }, csv.windowed(2).count { it == "\r\n" })
    }

    @Test
    fun rows_areNewestFirst_whateverOrderTheyComeIn() {
        val old = row(title = "Old", dateMillis = millisAt(2026, Calendar.JANUARY, 3))
        val mid = row(title = "Mid", dateMillis = millisAt(2026, Calendar.JUNE, 20))
        val new = row(title = "New", dateMillis = millisAt(2026, Calendar.OCTOBER, 11))

        val titles = buildCsv(listOf(mid, new, old)).lines().drop(1).filter { it.isNotEmpty() }
            .map { it.split(",")[3] }

        assertEquals(listOf("New", "Mid", "Old"), titles)
    }

    @Test
    fun date_isTheLocalDayInIsoForm() {
        // Late evening in India is still the same day there, though already the next one in Tokyo
        val lateEvening = millisAt(2026, Calendar.MARCH, 5, hour = 23, minute = 30)

        assertTrue(lineFor(row(dateMillis = lateEvening)).startsWith("2026-03-05,"))
    }

    @Test
    fun date_usesWesternDigits_whateverTheDeviceLanguage() {
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))

        assertTrue(lineFor(row()).startsWith("2026-10-11,"))
    }

    @Test
    fun type_isIncomeOrExpense() {
        assertEquals("Expense", lineFor(row(type = TransactionType.EXPENSE)).split(",")[1])
        assertEquals("Income", lineFor(row(type = TransactionType.INCOME)).split(",")[1])
    }

    @Test
    fun amount_isAPlainDecimalWithTwoPlaces() {
        fun amountOf(money: Money) = lineFor(row(amount = money)).substringAfterLast(",")

        assertEquals("1130.00", amountOf(rupees(1130)))
        assertEquals("100000.00", amountOf(rupees(100000)))
        assertEquals("250.50", amountOf(Money(25050)))
        assertEquals("0.05", amountOf(Money(5)))
        assertEquals("0.00", amountOf(Money.ZERO))
    }

    @Test
    fun amount_isNeverNegative() {
        assertEquals("250.50", lineFor(row(amount = Money(-25050))).substringAfterLast(","))
    }

    @Test
    fun aValueWithAComma_isQuoted() {
        assertEquals(
            "2026-10-11,Expense,Food,\"Tea, coffee\",,100.00",
            lineFor(row(title = "Tea, coffee"))
        )
    }

    @Test
    fun aValueWithAQuote_isQuoted_andTheQuoteDoubled() {
        assertEquals(
            "2026-10-11,Expense,Food,\"The \"\"good\"\" one\",,100.00",
            lineFor(row(title = "The \"good\" one"))
        )
    }

    @Test
    fun aValueWithALineBreak_isQuoted_andKeepsIt() {
        assertEquals(
            "2026-10-11,Expense,Food,Lunch,\"line one\nline two\",100.00",
            lineFor(row(note = "line one\nline two"))
        )
        assertEquals(
            "2026-10-11,Expense,Food,Lunch,\"line one\r\nline two\",100.00",
            lineFor(row(note = "line one\r\nline two"))
        )
    }

    @Test
    fun everyTextCell_isQuotedWhenItNeedsIt() {
        assertEquals(
            "2026-10-11,Expense,\"Food, drink\",\"a,b\",\"c,d\",100.00",
            lineFor(row(category = "Food, drink", title = "a,b", note = "c,d"))
        )
    }

    @Test
    fun aPlainValue_isNotQuoted() {
        assertEquals(
            "2026-10-11,Expense,Food & Dining,Swiggy order,it's fine; really,100.00",
            lineFor(row(category = "Food & Dining", title = "Swiggy order", note = "it's fine; really"))
        )
    }

    @Test
    fun anEmptyNote_isAnEmptyCell() {
        assertEquals("2026-10-11,Expense,Food,Lunch,,100.00", lineFor(row(note = "")))
    }

    @Test
    fun aCellThatStartsLikeAFormula_getsAnApostrophe() {
        listOf("=SUM(A1:A9)", "+91 98765", "-50 refund", "@home").forEach { text ->
            assertEquals("2026-10-11,Expense,Food,'$text,,100.00", lineFor(row(title = text)))
            assertEquals("2026-10-11,Expense,Food,Lunch,'$text,100.00", lineFor(row(note = text)))
            assertEquals("2026-10-11,Expense,'$text,Lunch,,100.00", lineFor(row(category = text)))
        }
    }

    @Test
    fun aCellThatStartsWithATabOrCarriageReturn_getsAnApostrophe() {
        assertEquals("2026-10-11,Expense,Food,'\t=1+1,,100.00", lineFor(row(title = "\t=1+1")))
        // The carriage return also means the cell is quoted
        assertEquals("2026-10-11,Expense,Food,\"'\r=1+1\",,100.00", lineFor(row(title = "\r=1+1")))
    }

    @Test
    fun aFormulaThatAlsoNeedsQuoting_getsBoth() {
        assertEquals(
            "2026-10-11,Expense,Food,\"'=HYPERLINK(\"\"http://x\"\",\"\"y\"\")\",,100.00",
            lineFor(row(title = "=HYPERLINK(\"http://x\",\"y\")"))
        )
    }

    @Test
    fun aFormulaCharacterLaterInTheCell_isLeftAlone() {
        assertEquals(
            "2026-10-11,Expense,Food,2+2=4,a-b@c,100.00",
            lineFor(row(title = "2+2=4", note = "a-b@c"))
        )
    }

    @Test
    fun unicode_isWrittenAsItIs() {
        assertEquals(
            "2026-10-11,Expense,किराना,चाय ☕ और समोसा,₹20 tip 😀,100.00",
            lineFor(row(category = "किराना", title = "चाय ☕ और समोसा", note = "₹20 tip 😀"))
        )
    }
}
