package com.example.expensetrackerpro.domain.quickadd

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.models.QuickAddResult
import com.example.expensetrackerpro.domain.models.TransactionType
import java.util.Calendar

/**
 * Turns a sentence such as "450 swiggy yesterday" into a draft transaction. The rules work on every
 * phone; another implementation (other languages, an on-device model) goes behind this interface,
 * and whatever it returns is still checked by `ParseQuickAddUseCase`.
 */
fun interface QuickAddParser {

    fun parse(input: QuickAddInput): QuickAddResult
}

data class QuickAddInput(
    val text: String,
    /** The current moment in the user's time zone: what "yesterday" counts from. */
    val today: Calendar,
    /** The form's type, kept when the sentence doesn't say income or expense. */
    val currentType: TransactionType,
    /** The user's categories of both types. A category is only picked among those of the result's type. */
    val categories: List<Category>,
    /** The categories the user picked for titles before. */
    val learned: List<CategoryMemory>,
    /** The symbol of the currency amounts are in, which marks a number as the amount, like "rs". */
    val currencySymbol: String
)
