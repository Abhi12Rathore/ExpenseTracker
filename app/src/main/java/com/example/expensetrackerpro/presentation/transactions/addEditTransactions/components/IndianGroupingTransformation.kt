package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.expensetrackerpro.utils.CurrencyFormatter

/** Shows a typed amount with en-IN grouping (`1,23,456.7`) while the field keeps plain digits. */
object IndianGroupingTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val grouped = CurrencyFormatter.groupInput(text.text)

        // Where each typed character, and the end of the text, lands once separators are added
        val groupedOffsets = IntArray(text.length + 1)
        var typed = 0
        grouped.forEachIndexed { index, char ->
            if (char != SEPARATOR) {
                groupedOffsets[typed] = index
                typed++
            }
        }
        groupedOffsets[text.length] = grouped.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = groupedOffsets[offset]

            override fun transformedToOriginal(offset: Int): Int =
                grouped.take(offset).count { it != SEPARATOR }
        }

        return TransformedText(AnnotatedString(grouped), offsetMapping)
    }

    private const val SEPARATOR = ','
}
