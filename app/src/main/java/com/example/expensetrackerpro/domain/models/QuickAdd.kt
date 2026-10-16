package com.example.expensetrackerpro.domain.models

/** How sure quick add is of a value it read from the user's sentence. */
enum class FieldConfidence {
    /** Said plainly: a number next to "₹", a date word, a category the user picked for this title before. */
    SURE,

    /** Worked out, and worth a look: a category from a keyword, the largest of several numbers. */
    GUESS,

    /** Not found. The field is left for the user. */
    MISSING
}

/** The parts of a sentence quick add looks for. */
enum class QuickAddField {
    AMOUNT, CATEGORY, TITLE, DATE, NOTE, TYPE
}

/**
 * What quick add understood: the [draft] that fills the form, and how sure it is of each part. A
 * field missing from [confidence] was neither found nor needed, e.g. no date word (the form keeps
 * today) or no word saying income or expense (the form keeps its type).
 */
data class QuickAddResult(
    val draft: TransactionDraft,
    val confidence: Map<QuickAddField, FieldConfidence>
) {
    fun confidenceOf(field: QuickAddField): FieldConfidence? = confidence[field]
}

/** A category the user chose for a title, remembered so that quick add picks it next time. */
data class CategoryMemory(
    /** The title as [CategoryMemory.keyOf] writes it, so "DMart" and "dmart " are one. */
    val titleKey: String,
    val type: TransactionType,
    val categoryId: CategoryId
) {
    companion object {
        private val NOT_WORD = Regex("""[^\p{L}\p{N}]+""")

        /** [title] in lowercase, with anything but letters and digits as single spaces between words. */
        fun keyOf(title: String): String = title.lowercase().replace(NOT_WORD, " ").trim()
    }
}
