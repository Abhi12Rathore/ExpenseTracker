package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.CurrencyChange
import kotlinx.coroutines.flow.StateFlow

/** The currency amounts are shown in, as the user chose it. Choosing one converts nothing. */
interface CurrencyRepository {

    /** INR until [load] has read the stored choice. */
    val currency: StateFlow<AppCurrency>

    /**
     * Reads the stored choice. On a fresh install there is none, so the phone region's currency
     * (else INR) is chosen and stored, and a later change of region doesn't change it.
     */
    suspend fun load()

    /** Fails with StorageFailure when the choice can't be stored; amounts then stay in the current currency. */
    suspend fun setCurrency(currency: AppCurrency): Outcome<Unit>

    /** The change that was last applied, for as long as it can be undone. Not kept across a restart. */
    val pendingChange: StateFlow<CurrencyChange?>

    fun setPendingChange(change: CurrencyChange?)
}
