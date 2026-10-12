package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.models.RecognizedText
import com.example.expensetrackerpro.domain.models.TextLine
import com.example.expensetrackerpro.domain.scan.BillScannerAvailability
import com.example.expensetrackerpro.domain.scan.BillTextReader
import kotlinx.coroutines.CompletableDeferred
import java.util.TimeZone

private const val DEFAULT_HEIGHT = 20
private const val LINE_SPACING = 30
private val HEIGHT_PREFIX = Regex("""^\[h=(\d+)] (.*)$""")

/** Midnight UTC of a date, which is how a draft and a date picker report one. */
fun utcDay(year: Int, month: Int, day: Int): Long =
    millisAt(year, month, day, timeZone = TimeZone.getTimeZone("UTC"))

/**
 * Lines of a bill as recognition would return them. A line starting with `[h=44] ` is printed
 * that tall; the rest are of ordinary height.
 */
fun recognizedText(vararg lines: String): RecognizedText =
    RecognizedText(
        lines.mapIndexed { index, line ->
            val match = HEIGHT_PREFIX.matchEntire(line)
            TextLine(
                text = match?.groupValues?.get(2) ?: line,
                top = index * LINE_SPACING,
                height = match?.groupValues?.get(1)?.toInt() ?: DEFAULT_HEIGHT
            )
        }
    )

/** The bill kept as `bills/[name].txt` in the test resources. */
fun billFixture(name: String): RecognizedText {
    val stream = checkNotNull(RecognizedText::class.java.classLoader?.getResourceAsStream("bills/$name.txt")) {
        "No bill fixture named $name"
    }

    return recognizedText(*stream.bufferedReader().use { it.readLines() }.toTypedArray())
}

class FakeBillTextReader(
    var result: Outcome<RecognizedText> = Outcome.Success(RecognizedText(emptyList()))
) : BillTextReader {

    /** Every photo asked for, in order. */
    val read = mutableListOf<DocumentUri>()

    /** When set, a read waits for it, as recognition takes a moment. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun read(image: DocumentUri): Outcome<RecognizedText> {
        read += image
        gate?.await()
        return result
    }
}

class FakeBillScannerAvailability(var available: Boolean = true) : BillScannerAvailability {

    override fun isAvailable(): Boolean = available
}
