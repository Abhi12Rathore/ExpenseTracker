package com.example.expensetrackerpro.domain.currency

/** How the whole part of an amount is split into groups of digits. */
enum class DigitGrouping {
    /** Last three digits, then groups of two: `12,34,567`. */
    INDIAN,

    /** Groups of three: `1,234,567`. */
    THOUSANDS
}

/**
 * A currency the app can show amounts in. Choosing one converts nothing: the stored amounts keep
 * their numbers and are only written with this currency's symbol, grouping and decimals.
 *
 * [code] is its ISO 4217 code. [symbol] is what is written next to an amount; where a short sign
 * would be ambiguous (several dollars, rupees and riyals) it carries its country, or is the code.
 * [badge] is the sign shown on the currency's row in the picker, as people there write it.
 * [fractionDigits] is how many decimals an amount is written with: 2, or 0 for a currency whose
 * minor unit is not in use. Money always holds hundredths of the unit, so no currency has more than 2.
 */
enum class AppCurrency(
    val code: String,
    val symbol: String,
    val badge: String,
    val fractionDigits: Int = 2,
    val grouping: DigitGrouping = DigitGrouping.THOUSANDS
) {
    INR(code = "INR", symbol = "₹", badge = "₹", grouping = DigitGrouping.INDIAN),
    USD(code = "USD", symbol = "$", badge = "$"),
    EUR(code = "EUR", symbol = "€", badge = "€"),
    GBP(code = "GBP", symbol = "£", badge = "£"),
    AED(code = "AED", symbol = "AED", badge = "د.إ"),
    SAR(code = "SAR", symbol = "SAR", badge = "ر.س"),
    QAR(code = "QAR", symbol = "QAR", badge = "ر.ق"),
    SGD(code = "SGD", symbol = "S$", badge = "S$"),
    AUD(code = "AUD", symbol = "A$", badge = "A$"),
    CAD(code = "CAD", symbol = "CA$", badge = "$"),
    NZD(code = "NZD", symbol = "NZ$", badge = "$"),
    JPY(code = "JPY", symbol = "¥", badge = "¥", fractionDigits = 0),
    CNY(code = "CNY", symbol = "CN¥", badge = "元"),
    HKD(code = "HKD", symbol = "HK$", badge = "HK$"),
    MYR(code = "MYR", symbol = "RM", badge = "RM"),
    THB(code = "THB", symbol = "฿", badge = "฿"),
    IDR(code = "IDR", symbol = "Rp", badge = "Rp"),
    NPR(code = "NPR", symbol = "रु", badge = "रु"),
    LKR(code = "LKR", symbol = "LKR", badge = "Rs"),
    BDT(code = "BDT", symbol = "৳", badge = "৳"),
    PKR(code = "PKR", symbol = "PKR", badge = "Rs"),
    ZAR(code = "ZAR", symbol = "R", badge = "R"),
    BRL(code = "BRL", symbol = "R$", badge = "R$"),
    CHF(code = "CHF", symbol = "CHF", badge = "Fr");

    companion object {
        /** The currency with ISO code [code], or null when the app doesn't offer it. */
        fun findByCode(code: String): AppCurrency? = entries.find { it.code == code.uppercase() }
    }
}
