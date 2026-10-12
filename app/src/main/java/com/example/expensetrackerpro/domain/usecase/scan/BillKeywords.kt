package com.example.expensetrackerpro.domain.usecase.scan

import com.example.expensetrackerpro.domain.models.CategoryHint
import com.example.expensetrackerpro.domain.models.Money

/**
 * Everything about reading a bill that depends on where it was printed: the words bills use and
 * the limits values are held to. The parsers know only the rules; bills of another country or
 * language need another instance of this, not other parsers.
 *
 * Words are lowercase and matched against a line with its punctuation removed, so "sub total"
 * also finds "Sub-Total:".
 */
data class BillKeywords(
    /** What the amount to pay is labelled with. */
    val totalLabels: List<TotalLabel>,
    /** A line with any of these holds some other figure, even if it also says "total". */
    val skipWords: List<String>,
    /** The skip words that may follow a strong total label, as in "Grand Total (incl. GST)". */
    val taxWords: List<String>,
    /** Lines with these hold numbers that are not amounts of the bill: phone, table, cash handed over. */
    val nonAmountWords: List<String>,
    /** Headings near the top that are not the merchant's name. */
    val titleStopWords: List<String>,
    /** What comes before a bill number, and the "no." after it: "Invoice No", "Bill number". */
    val billNumberLabels: List<String>,
    val numberWords: List<String>,
    /** With the bill number for `%s`. */
    val billNoteFormat: String,
    /** The twelve months as they are abbreviated, January first. Longer forms are matched too. */
    val months: List<String>,
    /** A word in the merchant's name, and what is usually bought there. The first match wins. */
    val categoryHints: List<Pair<String, CategoryHint>>,
    val titleMaxLength: Int,
    /** An amount this large or larger is taken for a misread. */
    val amountLimit: Money
) {

    /** [rank] orders the labels: when a bill has several, the highest is the one to pay. */
    data class TotalLabel(val words: String, val rank: Int)

    companion object {
        /** The rank from which a label means the final amount however the line goes on. */
        const val STRONG_RANK = 2

        val EN_IN = BillKeywords(
            totalLabels = listOf(
                TotalLabel("grand total", rank = 3),
                TotalLabel("net amount", rank = 2),
                TotalLabel("net amt", rank = 2),
                TotalLabel("net payable", rank = 2),
                TotalLabel("net total", rank = 2),
                TotalLabel("amount payable", rank = 2),
                TotalLabel("amt payable", rank = 2),
                TotalLabel("total payable", rank = 2),
                TotalLabel("to pay", rank = 2),
                TotalLabel("total", rank = 1)
            ),
            skipWords = listOf(
                "sub total", "subtotal", "gst", "cgst", "sgst", "igst", "tax", "taxes", "taxable",
                "discount", "saved", "you save", "savings", "items", "qty", "quantity",
                "round off", "roundoff"
            ),
            taxWords = listOf("gst", "cgst", "sgst", "igst", "tax", "taxes"),
            nonAmountWords = listOf(
                "ph", "phone", "tel", "mob", "mobile", "contact", "gstin", "fssai", "hsn", "pin",
                "table", "token", "kot", "order", "txn", "upi", "card", "cash", "tendered", "change"
            ),
            titleStopWords = listOf(
                "tax invoice", "invoice", "bill", "bill of supply", "gstin", "receipt", "cash memo",
                "original", "duplicate"
            ),
            billNumberLabels = listOf("bill", "invoice", "inv"),
            numberWords = listOf("no", "number", "num"),
            billNoteFormat = "Bill no. %s",
            months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"),
            categoryHints = listOf(
                "pharmacy" to CategoryHint.HEALTH,
                "pharma" to CategoryHint.HEALTH,
                "medical" to CategoryHint.HEALTH,
                "chemist" to CategoryHint.HEALTH,
                "hospital" to CategoryHint.HEALTH,
                "clinic" to CategoryHint.HEALTH,
                "petrol" to CategoryHint.FUEL,
                "fuel" to CategoryHint.FUEL,
                "hp" to CategoryHint.FUEL,
                "iocl" to CategoryHint.FUEL,
                "bpcl" to CategoryHint.FUEL,
                "hpcl" to CategoryHint.FUEL,
                "indian oil" to CategoryHint.FUEL,
                "restaurant" to CategoryHint.FOOD,
                "cafe" to CategoryHint.FOOD,
                "coffee" to CategoryHint.FOOD,
                "swiggy" to CategoryHint.FOOD,
                "zomato" to CategoryHint.FOOD,
                "kitchen" to CategoryHint.FOOD,
                "bakery" to CategoryHint.FOOD,
                "dhaba" to CategoryHint.FOOD,
                "pizza" to CategoryHint.FOOD,
                "mart" to CategoryHint.GROCERIES,
                "bazaar" to CategoryHint.GROCERIES,
                "grocery" to CategoryHint.GROCERIES,
                "fresh" to CategoryHint.GROCERIES,
                "supermarket" to CategoryHint.GROCERIES,
                "kirana" to CategoryHint.GROCERIES,
                "amazon" to CategoryHint.SHOPPING,
                "flipkart" to CategoryHint.SHOPPING,
                "myntra" to CategoryHint.SHOPPING,
                "fashion" to CategoryHint.SHOPPING,
                "uber" to CategoryHint.TRANSPORT,
                "ola" to CategoryHint.TRANSPORT,
                "metro" to CategoryHint.TRANSPORT,
                "cinema" to CategoryHint.ENTERTAINMENT,
                "pvr" to CategoryHint.ENTERTAINMENT,
                "inox" to CategoryHint.ENTERTAINMENT,
                "electricity" to CategoryHint.BILLS,
                "broadband" to CategoryHint.BILLS,
                "airlines" to CategoryHint.TRAVEL,
                "irctc" to CategoryHint.TRAVEL
            ),
            titleMaxLength = 40,
            amountLimit = Money.ofRupees(1_000_000)
        )
    }
}
