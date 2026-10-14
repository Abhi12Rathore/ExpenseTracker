package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.currency.AppCurrency
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

    suspend fun setCurrency(currency: AppCurrency)

    /** The change that was last applied, for as long as it can be undone. Not kept across a restart. */
    val pendingChange: StateFlow<CurrencyChange?>

    fun setPendingChange(change: CurrencyChange?)
}
