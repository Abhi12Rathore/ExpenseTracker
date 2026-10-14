package com.example.expensetrackerpro.domain.currency

/** The characters a language writes numbers with: `.` and `,` in English, `,` and `.` in German. */
data class NumberSymbols(
    val decimalSeparator: Char,
    val groupingSeparator: Char
) {
    companion object {
        /** English's: `1,234.50`. */
        val ENGLISH = NumberSymbols(decimalSeparator = '.', groupingSeparator = ',')
    }
}

/** Where a language writes a currency's symbol. */
enum class SymbolPlacement {
    /** `$100.00`, or `R$ 100,00` and `AED 100.00` with a space. */
    BEFORE,

    /** `100,00 €`, always with a space. */
    AFTER
}
