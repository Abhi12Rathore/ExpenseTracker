package com.example.expensetrackerpro.domain.usecase.applock

import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.domain.models.LockTimeout
import com.example.expensetrackerpro.fakes.FakeAppLockRepository
import com.example.expensetrackerpro.fakes.FakeBiometricAvailability
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLockUseCasesTest {

    @Test
    fun getAppLockSettings_nothingSaved_isOffWithThirtySeconds() = runTest {
        assertEquals(
            AppLockSettings(enabled = false, timeout = LockTimeout.SECONDS_30),
            GetAppLockSettingsUseCase(FakeAppLockRepository())().first()
        )
    }

    @Test
    fun setAppLockSettings_isSeenByGetAppLockSettings() = runTest {
        val repository = FakeAppLockRepository()
        val settings = AppLockSettings(enabled = true, timeout = LockTimeout.MINUTES_5)

        SetAppLockSettingsUseCase(repository)(settings)

        assertEquals(settings, repository.current)
        assertEquals(settings, GetAppLockSettingsUseCase(repository)().first())
    }

    @Test
    fun getBiometricStatus_readsTheCurrentStatusEachTime() {
        val availability = FakeBiometricAvailability(BiometricStatus.NO_DEVICE_CREDENTIAL)
        val useCase = GetBiometricStatusUseCase(availability)

        assertEquals(BiometricStatus.NO_DEVICE_CREDENTIAL, useCase())

        availability.current = BiometricStatus.AVAILABLE

        assertEquals(BiometricStatus.AVAILABLE, useCase())
    }

    @Test
    fun getCurrentTimeMillis_followsTheClock() {
        val timeProvider = FakeTimeProvider(nowMillis = 5_000L)
        val useCase = GetCurrentTimeMillisUseCase(timeProvider)

        assertEquals(5_000L, useCase())

        timeProvider.nowMillis = 9_000L

        assertEquals(9_000L, useCase())
    }
}
