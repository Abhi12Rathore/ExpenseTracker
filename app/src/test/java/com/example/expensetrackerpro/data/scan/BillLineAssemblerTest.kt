package com.example.expensetrackerpro.data.scan

import com.example.expensetrackerpro.domain.models.RecognizedText
import com.example.expensetrackerpro.domain.models.TextLine
import org.junit.Assert.assertEquals
import org.junit.Test

class BillLineAssemblerTest {

    private val assembler = BillLineAssembler()

    @Test
    fun piecesAtTheSameHeight_becomeOneLine_leftToRight() {
        // Recognition returns the column of labels first, then the column of amounts
        val text = assembler.assemble(
            listOf(
                TextFragment("Sub Total", left = 40, top = 300, height = 20),
                TextFragment("Grand Total", left = 40, top = 332, height = 22),
                TextFragment("1360.00", left = 420, top = 302, height = 20),
                TextFragment("₹1,428.00", left = 410, top = 330, height = 22)
            )
        )

        assertEquals(
            RecognizedText(
                listOf(
                    TextLine("Sub Total 1360.00", top = 300, height = 20),
                    TextLine("Grand Total ₹1,428.00", top = 330, height = 22)
                )
            ),
            text
        )
    }

    @Test
    fun linesComeTopToBottom_whateverOrderTheyWereRecognizedIn() {
        val text = assembler.assemble(
            listOf(
                TextFragment("Thank you", left = 100, top = 500, height = 18),
                TextFragment("CAFE COFFEE DAY", left = 60, top = 20, height = 44),
                TextFragment("MG Road", left = 90, top = 70, height = 20)
            )
        )

        assertEquals(listOf("CAFE COFFEE DAY", "MG Road", "Thank you"), text.lines.map { it.text })
        assertEquals(listOf(44, 20, 18), text.lines.map { it.height })
    }

    @Test
    fun aSmallPieceBesideLargePrint_joinsItsLine_butTheNextLineDoesNot() {
        val text = assembler.assemble(
            listOf(
                TextFragment("TOTAL", left = 40, top = 100, height = 40),
                TextFragment("525.00", left = 300, top = 110, height = 24),
                TextFragment("Paid by UPI", left = 40, top = 146, height = 20)
            )
        )

        assertEquals(listOf("TOTAL 525.00", "Paid by UPI"), text.lines.map { it.text })
    }

    @Test
    fun blankPieces_areLeftOut_andNoPiecesGiveNoLines() {
        assertEquals(RecognizedText(emptyList()), assembler.assemble(emptyList()))
        assertEquals(
            listOf("Total"),
            assembler.assemble(
                listOf(TextFragment("  ", 0, 0, 20), TextFragment(" Total ", 10, 40, 20))
            ).lines.map { it.text }
        )
    }
}
