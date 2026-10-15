package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.expensetrackerpro.utils.CurrencyFormatter

/**
 * Shows a typed amount as [formatter] writes it in the app's currency and language (`1,23,456.7` for
 * rupees in English, `123.456,7` for euros in German)
 * while the field keeps plain digits.
 */
class AmountGroupingTransformation(private val formatter: CurrencyFormatter) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val grouped = formatter.groupInput(text.text)
        val separator = formatter.groupingSeparator

        // Where each typed character, and the end of the text, lands once separators are added
        val groupedOffsets = IntArray(text.length + 1)
        var typed = 0
        grouped.forEachIndexed { index, char ->
            if (char != separator) {
                groupedOffsets[typed] = index
                typed++
            }
        }
        groupedOffsets[text.length] = grouped.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = groupedOffsets[offset]

            override fun transformedToOriginal(offset: Int): Int =
                grouped.take(offset).count { it != separator }
        }

        return TransformedText(AnnotatedString(grouped), offsetMapping)
    }
}
