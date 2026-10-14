package com.example.expensetrackerpro.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.CurrencyChange
import com.example.expensetrackerpro.fakes.FakeRegionCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class CurrencyRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val region = FakeRegionCurrency()

    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFile("settings.preferences_pb").also { it.delete() }
        }

    @Test
    fun beforeItIsLoaded_amountsAreInRupees() = runTest {
        val repository = CurrencyRepositoryImpl(dataStore(), region)

        assertEquals(AppCurrency.INR, repository.currency.value)
        assertEquals(AppCurrency.INR, repository.current())
    }

    @Test
    fun aFreshInstall_takesThePhoneRegionsCurrency_andStoresIt() = runTest {
        val dataStore = dataStore()
        region.currency = AppCurrency.USD

        CurrencyRepositoryImpl(dataStore, region).load()

        assertEquals("USD", dataStore.data.first()[CurrencyRepositoryImpl.CURRENCY_CODE])
    }

    @Test
    fun aFreshInstall_inARegionWhoseCurrencyTheAppDoesntOffer_isInRupees() = runTest {
        val repository = CurrencyRepositoryImpl(dataStore(), region)
        region.currency = null

        repository.load()

        assertEquals(AppCurrency.INR, repository.current())
    }

    @Test
    fun theStoredChoice_winsOverTheRegion_whichMayHaveChangedSince() = runTest {
        val dataStore = dataStore()
        region.currency = AppCurrency.USD
        CurrencyRepositoryImpl(dataStore, region).load()

        // The phone moves to Germany
        region.currency = AppCurrency.EUR
        val repository = CurrencyRepositoryImpl(dataStore, region)
        repository.load()

        assertEquals(AppCurrency.USD, repository.current())
    }

    @Test
    fun aChosenCurrency_isStored_andReadAgainAtTheNextStart() = runTest {
        val dataStore = dataStore()
        val repository = CurrencyRepositoryImpl(dataStore, region)
        repository.load()

        repository.setCurrency(AppCurrency.JPY)

        assertEquals(AppCurrency.JPY, repository.currency.value)
        val nextStart = CurrencyRepositoryImpl(dataStore, region)
        nextStart.load()
        assertEquals(AppCurrency.JPY, nextStart.current())
    }

    @Test
    fun aStoredCodeTheAppNoLongerOffers_fallsBackToTheDefault() = runTest {
        val dataStore = dataStore()
        dataStore.edit { it[CurrencyRepositoryImpl.CURRENCY_CODE] = "KWD" }
        region.currency = AppCurrency.GBP
        val repository = CurrencyRepositoryImpl(dataStore, region)

        repository.load()

        assertEquals(AppCurrency.GBP, repository.current())
    }

    @Test
    fun unreadablePreferences_fallBackToTheDefault_withoutFailing() = runTest {
        val unreadable = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt file") }

            override suspend fun updateData(
                transform: suspend (t: Preferences) -> Preferences
            ): Preferences = throw IOException("corrupt file")
        }
        region.currency = AppCurrency.EUR
        val repository = CurrencyRepositoryImpl(unreadable, region)

        repository.load()

        assertEquals(AppCurrency.EUR, repository.current())
    }

    @Test
    fun thePendingChange_isKeptInMemoryOnly() = runTest {
        val dataStore = dataStore()
        val repository = CurrencyRepositoryImpl(dataStore, region)
        val change = CurrencyChange(previous = AppCurrency.INR, current = AppCurrency.USD)

        repository.setPendingChange(change)
        assertEquals(change, repository.pendingChange.value)

        assertNull(CurrencyRepositoryImpl(dataStore, region).pendingChange.value)
    }
}
