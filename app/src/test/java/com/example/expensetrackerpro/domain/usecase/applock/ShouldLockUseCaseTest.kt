package com.example.expensetrackerpro.domain.usecase.applock

import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.LockTimeout
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShouldLockUseCaseTest {

    private val backgroundedAt = 1_000_000L

    private fun shouldLock(
        elapsedMillis: Long,
        timeout: LockTimeout,
        enabled: Boolean = true
    ): Boolean = ShouldLockUseCase(FakeTimeProvider(backgroundedAt + elapsedMillis))(
        AppLockSettings(enabled = enabled, timeout = timeout),
        backgroundedAt
    )

    @Test
    fun lockDisabled_neverLocks() {
        LockTimeout.entries.forEach { timeout ->
            assertFalse(shouldLock(elapsedMillis = timeout.millis + 1, timeout, enabled = false))
        }
    }

    @Test
    fun neverBackgrounded_doesNotLock() {
        val useCase = ShouldLockUseCase(FakeTimeProvider(backgroundedAt))

        LockTimeout.entries.forEach { timeout ->
            assertFalse(useCase(AppLockSettings(enabled = true, timeout = timeout), null))
        }
    }

    @Test
    fun immediately_locksEvenWithNoTimePassed() {
        assertTrue(shouldLock(elapsedMillis = 0, LockTimeout.IMMEDIATELY))
        assertTrue(shouldLock(elapsedMillis = 1, LockTimeout.IMMEDIATELY))
    }

    @Test
    fun thirtySeconds_locksFromExactlyThirtySeconds() {
        assertFalse(shouldLock(elapsedMillis = 0, LockTimeout.SECONDS_30))
        assertFalse(shouldLock(elapsedMillis = 29_999, LockTimeout.SECONDS_30))
        assertTrue(shouldLock(elapsedMillis = 30_000, LockTimeout.SECONDS_30))
        assertTrue(shouldLock(elapsedMillis = 30_001, LockTimeout.SECONDS_30))
    }

    @Test
    fun oneMinute_locksFromExactlyOneMinute() {
        assertFalse(shouldLock(elapsedMillis = 59_999, LockTimeout.MINUTE_1))
        assertTrue(shouldLock(elapsedMillis = 60_000, LockTimeout.MINUTE_1))
        assertTrue(shouldLock(elapsedMillis = 60_001, LockTimeout.MINUTE_1))
    }

    @Test
    fun fiveMinutes_locksFromExactlyFiveMinutes() {
        assertFalse(shouldLock(elapsedMillis = 299_999, LockTimeout.MINUTES_5))
        assertTrue(shouldLock(elapsedMillis = 300_000, LockTimeout.MINUTES_5))
        assertTrue(shouldLock(elapsedMillis = 300_001, LockTimeout.MINUTES_5))
    }

    @Test
    fun clockMovedBackwards_locks() {
        LockTimeout.entries.forEach { timeout ->
            assertTrue(shouldLock(elapsedMillis = -1, timeout))
        }
    }

    @Test
    fun timeoutDurations() {
        assertEquals(
            listOf(0L, 30_000L, 60_000L, 300_000L),
            LockTimeout.entries.map { it.millis }
        )
    }
}
