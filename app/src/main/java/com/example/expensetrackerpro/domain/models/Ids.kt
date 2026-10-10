package com.example.expensetrackerpro.domain.models

// Typed so that one kind of id can't be handed over where the other is expected

@JvmInline
value class TransactionId(val value: Int) {
    companion object {
        /** A transaction that is not stored yet; the database gives it its id. */
        val UNSAVED = TransactionId(0)
    }
}

@JvmInline
value class CategoryId(val value: Int) {
    companion object {
        /** A category that is not stored yet; the database gives it its id. */
        val UNSAVED = CategoryId(0)
    }
}
