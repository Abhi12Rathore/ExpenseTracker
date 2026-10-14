package com.example.expensetrackerpro.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.currency.CurrencyProvider
import com.example.expensetrackerpro.domain.currency.RegionCurrency
import com.example.expensetrackerpro.domain.models.CurrencyChange
import com.example.expensetrackerpro.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The chosen currency, stored in DataStore by its ISO code and held in memory once read, so
 * [CurrencyProvider.current] answers at once on every amount drawn. The app reads it under the
 * splash, before anything is shown.
 */
@Singleton
class CurrencyRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val regionCurrency: RegionCurrency
) : CurrencyRepository, CurrencyProvider {

    private val _currency = MutableStateFlow(AppCurrency.INR)
    override val currency: StateFlow<AppCurrency> = _currency.asStateFlow()

    private val _pendingChange = MutableStateFlow<CurrencyChange?>(null)
    override val pendingChange: StateFlow<CurrencyChange?> = _pendingChange.asStateFlow()

    override fun current(): AppCurrency = _currency.value

    override suspend fun load() {
        val preferences = dataStore.data
            // An unreadable preferences file falls back to the defaults instead of failing the app
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .first()
        val stored = preferences[CURRENCY_CODE]?.let(AppCurrency::findByCode)

        if (stored != null) {
            _currency.value = stored
            return
        }

        val default = regionCurrency.currency() ?: AppCurrency.INR
        _currency.value = default
        // Stored at once, so the currency stays what it was when the app was first opened. A file
        // that can't be written only means the default is chosen again at the next start
        try {
            dataStore.edit { preferences -> preferences[CURRENCY_CODE] = default.code }
        } catch (_: IOException) {
            return
        }
    }

    override suspend fun setCurrency(currency: AppCurrency) {
        dataStore.edit { preferences -> preferences[CURRENCY_CODE] = currency.code }
        _currency.value = currency
    }

    override fun setPendingChange(change: CurrencyChange?) {
        _pendingChange.value = change
    }

    internal companion object {
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
    }
}
