package com.example.expensetrackerpro.domain.usecase.scan

import com.example.expensetrackerpro.domain.models.CategoryHint
import com.example.expensetrackerpro.domain.models.RecognizedText
import com.example.expensetrackerpro.domain.models.TextLine
import com.example.expensetrackerpro.domain.models.TransactionDraft
import java.util.Calendar
import javax.inject.Inject

/**
 * Works out a transaction from the text of a bill: the amount to pay, the merchant as the title,
 * the date, the bill number as a note and what kind of spending it is. What is doubtful is left
 * out, since a wrong value is worse than an empty field.
 */
class ParseBillUseCase(private val keywords: BillKeywords) {

    @Inject
    constructor() : this(BillKeywords.EN_IN)

    private val amountParser = BillAmountParser(keywords)
    private val titleParser = BillTitleParser(keywords)
    private val dateParser = BillDateParser(keywords)
    private val noteParser = BillNoteParser(keywords)

    /** [today] is the current moment in the user's time zone, which the date is checked against. */
    operator fun invoke(text: RecognizedText, today: Calendar): TransactionDraft {
        val lines = text.lines.filter { it.text.isNotBlank() }
        val texts = lines.map { BillTokens.fixDigits(it.text) }
        val amounts = amountParser.parse(texts.map { dateParser.strip(noteParser.strip(it)) })

        return TransactionDraft(
            amount = amounts.amount,
            otherAmounts = amounts.others,
            // From the text as scanned: a name has no digits to put right
            title = titleParser.parse(lines),
            dateMillis = dateParser.parse(texts, today),
            note = noteParser.parse(texts),
            categoryHint = categoryHint(lines)
        )
    }

    /** From the top of the bill, where the merchant's name and trade are printed. */
    private fun categoryHint(lines: List<TextLine>): CategoryHint? {
        val words = BillTokens.words(lines.take(HINT_LINES).joinToString(" ") { it.text })

        return keywords.categoryHints.firstOrNull { (keyword, _) ->
            // A short word such as "hp" only on its own; a longer one also inside a name, as in "DMart"
            if (keyword.length <= SHORT_KEYWORD) BillTokens.hasPhrase(words, keyword) else keyword in words
        }?.second
    }

    private companion object {
        const val HINT_LINES = 5
        const val SHORT_KEYWORD = 3
    }
}
