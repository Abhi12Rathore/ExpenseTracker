package com.example.expensetrackerpro.domain.usecase.scan

/** Finds the bill number after "Bill No", "Invoice No" or "Inv No", and words the note from it. */
internal class BillNoteParser(private val keywords: BillKeywords) {

    // The label, "no." or "#", then the number, which may hold letters, slashes and dashes
    private val billNumber = Regex(
        """(?<!\p{L})(?:${alternatives(keywords.billNumberLabels)})\.?\s*(?:(?:${alternatives(keywords.numberWords)})(?!\p{L})|#)\s*[.:#-]*\s*([\p{L}\d][\p{L}\d/-]*)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(lines: List<String>): String? =
        lines.firstNotNullOfOrNull(::numberIn)?.let { keywords.billNoteFormat.format(it) }

    /** [line] without its bill number, whose digits would otherwise read as an amount. */
    fun strip(line: String): String = billNumber.replace(line, " ")

    // "Bill No: Cash" is a label followed by some other word, not a number
    private fun numberIn(line: String): String? =
        billNumber.findAll(line).map { it.groupValues[1] }.firstOrNull { it.any(Char::isDigit) }

    private fun alternatives(words: List<String>): String = words.joinToString("|", transform = Regex::escape)
}
