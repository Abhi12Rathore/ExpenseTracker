package com.example.expensetrackerpro.data.scan

import com.example.expensetrackerpro.domain.models.RecognizedText
import com.example.expensetrackerpro.domain.models.TextLine
import javax.inject.Inject

/** A piece of text as recognized, with where it is in the photo, in pixels. */
data class TextFragment(val text: String, val left: Int, val top: Int, val height: Int)

/**
 * Puts recognized text back into the lines of the bill. Recognition returns the columns of a
 * bill as separate pieces, a label in one and its amount in another, so pieces at the same
 * height are joined left to right into the line a person would read.
 */
class BillLineAssembler @Inject constructor() {

    fun assemble(fragments: List<TextFragment>): RecognizedText {
        val rows = mutableListOf<MutableList<TextFragment>>()

        fragments
            .filter { it.text.isNotBlank() }
            .sortedBy { it.centerY }
            .forEach { fragment ->
                val row = rows.lastOrNull()?.takeIf { isOnRow(fragment, it) }
                if (row != null) row += fragment else rows += mutableListOf(fragment)
            }

        return RecognizedText(
            rows.map { row ->
                TextLine(
                    text = row.sortedBy { it.left }.joinToString(" ") { it.text.trim() },
                    top = row.minOf { it.top },
                    height = row.maxOf { it.height }
                )
            }
        )
    }

    // On the row when its middle is within half a line of the row's first piece
    private fun isOnRow(fragment: TextFragment, row: List<TextFragment>): Boolean {
        val first = row.first()
        return kotlin.math.abs(fragment.centerY - first.centerY) * 2 < minOf(fragment.height, first.height)
    }

    private val TextFragment.centerY: Int get() = top + height / 2
}
