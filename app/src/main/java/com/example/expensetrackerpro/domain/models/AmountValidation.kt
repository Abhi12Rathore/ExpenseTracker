package com.example.expensetrackerpro.domain.models


/** What a typed amount turned out to be. */
sealed interface AmountValidation {
    data class Valid(val amount: Money) : AmountValidation
    data class Invalid(val reason: AmountError) : AmountValidation
}

enum class AmountError {
    EMPTY,
    MALFORMED,
    TOO_MANY_DECIMALS,
    NOT_POSITIVE,
    ABOVE_LIMIT
}
