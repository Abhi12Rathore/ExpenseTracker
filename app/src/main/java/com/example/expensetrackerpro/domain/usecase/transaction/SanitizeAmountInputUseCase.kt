package com.example.expensetrackerpro.domain.usecase.transaction

import java.math.BigDecimal
import javax.inject.Inject

/**
 * Filters the amount field as it is edited. An edit is kept only while the text can still become
 * a valid amount (digits, one decimal point, two decimals, within the limit); anything else,
 * such as pasted text, leaves the field as it was.
 */
class SanitizeAmountInputUseCase @Inject constructor() {

    operator fun invoke(previous: String, incoming: String): String {
        if (incoming.isEmpty()) return ""

        val match = PARTIAL.matchEntire(incoming) ?: return previous
        // "007" reads as 7, and a bare "." as "0."
        val integerPart = match.groupValues[1].trimStart('0').ifEmpty { "0" }
        val sanitized = integerPart + match.groupValues[2]

        return if (BigDecimal(sanitized) > ValidateAmountUseCase.MAX_AMOUNT) previous else sanitized
    }

    private companion object {
        val PARTIAL = Regex("""([0-9]*)(\.[0-9]{0,2})?""")
    }
}
