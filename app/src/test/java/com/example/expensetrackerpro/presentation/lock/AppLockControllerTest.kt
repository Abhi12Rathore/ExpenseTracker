package com.example.expensetrackerpro.presentation.lock

import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.domain.models.LockTimeout
import com.example.expensetrackerpro.domain.repository.AppLockRepository
import com.example.expensetrackerpro.domain.usecase.applock.GetAppLockSettingsUseCase
import com.example.expensetrackerpro.domain.usecase.applock.GetBiometricStatusUseCase
import com.example.expensetrackerpro.domain.usecase.applock.GetCurrentTimeMillisUseCase
import com.example.expensetrackerpro.domain.usecase.applock.ShouldLockUseCase
import com.example.expensetrackerpro.fakes.FakeAppLockRepository
import com.example.expensetrackerpro.fakes.FakeBiometricAvailability
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockControllerTest {

    private val timeProvider = FakeTimeProvider(nowMillis = 1_000_000L)
    private val biometricAvailability = FakeBiometricAvailability()

    /** A controller already following [appLockRepository], as one that a view model has started. */
    private fun TestScope.controller(
        appLockRepository: AppLockRepository = FakeAppLockRepository()
    ) = AppLockController(
        GetAppLockSettingsUseCase(appLockRepository),
        GetBiometricStatusUseCase(biometricAvailability),
        ShouldLockUseCase(timeProvider),
        GetCurrentTimeMillisUseCase(timeProvider)
    ).apply { start(backgroundScope + UnconfinedTestDispatcher(testScheduler)) }

    private fun TestScope.lockedController(timeout: LockTimeout = LockTimeout.SECONDS_30) =
        controller(FakeAppLockRepository(AppLockSettings(true, timeout)))

    /** A controller with the lock on that has been unlocked, as after a normal launch. */
    private fun TestScope.unlockedController(timeout: LockTimeout = LockTimeout.SECONDS_30) =
        lockedController(timeout).apply {
            onPromptLaunched()
            onAuthFinished(AuthResult.SUCCESS)
        }

    private fun AppLockController.backgroundFor(millis: Long) {
        onAppBackgrounded()
        timeProvider.nowMillis += millis
        onAppForegrounded()
    }

    @Test
    fun notReadyUntilTheAppLockSettingsAreLoaded() = runTest {
        val appLockRepository = PendingAppLockRepository()
        val controller = controller(appLockRepository)

        assertFalse(controller.state.value.isLockStateKnown)
        assertFalse(controller.state.value.isLocked)

        appLockRepository.emit(AppLockSettings(enabled = true))

        assertTrue(controller.state.value.isLockStateKnown)
        assertTrue(controller.state.value.isLocked)
    }

    @Test
    fun nothingIsKnown_untilTheControllerIsStarted() = runTest {
        val controller = AppLockController(
            GetAppLockSettingsUseCase(FakeAppLockRepository(AppLockSettings(enabled = true))),
            GetBiometricStatusUseCase(biometricAvailability),
            ShouldLockUseCase(timeProvider),
            GetCurrentTimeMillisUseCase(timeProvider)
        )

        assertEquals(AppLockState(), controller.state.value)
    }

    @Test
    fun lockOff_startsUnlocked() = runTest {
        val state = controller().state.value

        assertFalse(state.isLocked)
        assertFalse(state.isAppLockEnabled)
        assertFalse(state.isPromptRequested)
    }

    @Test
    fun lockOn_startsLockedAndAsksForThePrompt() = runTest {
        // Even the longest timeout locks on launch: a new process has no session to continue
        val state = lockedController(LockTimeout.MINUTES_5).state.value

        assertTrue(state.isLocked)
        assertTrue(state.isAppLockEnabled)
        assertTrue(state.isPromptRequested)
        assertNull(state.lockMessage)
    }

    @Test
    fun lockOnButNoScreenLock_startsUnlocked() = runTest {
        biometricAvailability.current = BiometricStatus.NO_DEVICE_CREDENTIAL

        val state = lockedController().state.value

        assertFalse(state.isLocked)
        assertTrue(state.isAppLockEnabled)
    }

    @Test
    fun promptLaunched_consumesTheRequest() = runTest {
        val controller = lockedController()

        controller.onPromptLaunched()

        assertFalse(controller.state.value.isPromptRequested)
        assertTrue(controller.state.value.isLocked)
    }

    @Test
    fun authSucceeded_unlocks() = runTest {
        val controller = lockedController()
        controller.onPromptLaunched()

        controller.onAuthFinished(AuthResult.SUCCESS)

        assertFalse(controller.state.value.isLocked)
        assertFalse(controller.state.value.isPromptRequested)
        assertTrue(controller.state.value.isAppLockEnabled)
    }

    @Test
    fun authFailures_stayLockedWithAMessage() = runTest {
        mapOf(
            AuthResult.CANCELED to LockMessage.CANCELED,
            AuthResult.LOCKOUT to LockMessage.LOCKOUT,
            AuthResult.LOCKOUT_PERMANENT to LockMessage.LOCKOUT_PERMANENT,
            AuthResult.ERROR to LockMessage.ERROR
        ).forEach { (result, message) ->
            val controller = lockedController()
            controller.onPromptLaunched()

            controller.onAuthFinished(result)

            assertTrue(controller.state.value.isLocked)
            assertEquals(message, controller.state.value.lockMessage)
            assertFalse(controller.state.value.isPromptRequested)
        }
    }

    @Test
    fun screenLockRemovedWhileLocked_unlocksInsteadOfStrandingTheUser() = runTest {
        val controller = lockedController()
        controller.onPromptLaunched()

        controller.onAuthFinished(AuthResult.NO_CREDENTIAL)

        assertFalse(controller.state.value.isLocked)
    }

    @Test
    fun unlockClicked_clearsTheMessageAndAsksForThePromptAgain() = runTest {
        val controller = lockedController()
        controller.onPromptLaunched()
        controller.onAuthFinished(AuthResult.LOCKOUT)

        controller.onUnlockClicked()

        assertTrue(controller.state.value.isPromptRequested)
        assertNull(controller.state.value.lockMessage)
        assertTrue(controller.state.value.isLocked)
    }

    @Test
    fun unlockClickedWhileUnlocked_doesNothing() = runTest {
        val controller = unlockedController()

        controller.onUnlockClicked()

        assertFalse(controller.state.value.isPromptRequested)
    }

    @Test
    fun backInsideTheTimeout_staysUnlocked() = runTest {
        val controller = unlockedController(LockTimeout.SECONDS_30)

        controller.backgroundFor(29_999)

        assertFalse(controller.state.value.isLocked)
    }

    @Test
    fun backExactlyAtTheTimeout_locks() = runTest {
        val controller = unlockedController(LockTimeout.SECONDS_30)

        controller.backgroundFor(30_000)

        assertTrue(controller.state.value.isLocked)
        assertTrue(controller.state.value.isPromptRequested)
    }

    @Test
    fun backAfterTheTimeout_locks() = runTest {
        val controller = unlockedController(LockTimeout.MINUTE_1)

        controller.backgroundFor(60_001)

        assertTrue(controller.state.value.isLocked)
    }

    @Test
    fun immediately_locksOnEveryReturn() = runTest {
        val controller = unlockedController(LockTimeout.IMMEDIATELY)

        controller.backgroundFor(0)

        assertTrue(controller.state.value.isLocked)
    }

    @Test
    fun foregroundedWithoutEverBackgrounding_doesNotLock() = runTest {
        // What a rotation looks like: the new activity's observer is told the app is in front
        val controller = unlockedController(LockTimeout.IMMEDIATELY)

        controller.onAppForegrounded()

        assertFalse(controller.state.value.isLocked)
    }

    @Test
    fun eachReturnIsMeasuredFromItsOwnBackgrounding() = runTest {
        val controller = unlockedController(LockTimeout.SECONDS_30)
        controller.backgroundFor(20_000)

        controller.backgroundFor(20_000)

        assertFalse(controller.state.value.isLocked)
    }

    @Test
    fun lockOff_neverLocksOnReturn() = runTest {
        val controller = controller()

        controller.backgroundFor(600_000)

        assertFalse(controller.state.value.isLocked)
    }

    @Test
    fun noScreenLock_doesNotLockOnReturn() = runTest {
        val controller = unlockedController(LockTimeout.IMMEDIATELY)
        biometricAvailability.current = BiometricStatus.NO_DEVICE_CREDENTIAL

        controller.backgroundFor(1_000)

        assertFalse(controller.state.value.isLocked)
    }

    @Test
    fun unlockingThroughAScreenThatBackgroundsTheApp_doesNotLockAgain() = runTest {
        // Before Android 11 the screen lock check is a separate screen: the result arrives first,
        // and the app only then learns it is back in front
        val controller = lockedController(LockTimeout.IMMEDIATELY)
        controller.onPromptLaunched()
        controller.onAppBackgrounded()
        timeProvider.nowMillis += 5_000

        controller.onAuthFinished(AuthResult.SUCCESS)
        controller.onAppForegrounded()

        assertFalse(controller.state.value.isLocked)
    }

    @Test
    fun returningWhileThePromptIsOpen_doesNotAskForAnotherPrompt() = runTest {
        val controller = lockedController(LockTimeout.IMMEDIATELY)
        controller.onPromptLaunched()

        controller.backgroundFor(5_000)

        assertTrue(controller.state.value.isLocked)
        assertFalse(controller.state.value.isPromptRequested)
    }

    @Test
    fun returningToTheLockScreenAfterACancelledPrompt_asksForThePromptAgain() = runTest {
        val controller = lockedController()
        controller.onPromptLaunched()
        controller.onAuthFinished(AuthResult.CANCELED)

        controller.backgroundFor(1_000)

        assertTrue(controller.state.value.isPromptRequested)
        assertNull(controller.state.value.lockMessage)
    }

    @Test
    fun turningTheLockOnMidSession_doesNotLockUntilTheNextTimeout() = runTest {
        val appLockRepository = FakeAppLockRepository()
        val controller = controller(appLockRepository)

        appLockRepository.setSettings(AppLockSettings(enabled = true, timeout = LockTimeout.MINUTE_1))

        assertFalse(controller.state.value.isLocked)
        assertTrue(controller.state.value.isAppLockEnabled)

        controller.backgroundFor(60_000)

        assertTrue(controller.state.value.isLocked)
    }

    @Test
    fun aChangedTimeoutAppliesToTheNextReturn() = runTest {
        val appLockRepository = FakeAppLockRepository(AppLockSettings(true, LockTimeout.MINUTES_5))
        val controller = controller(appLockRepository)
        controller.onPromptLaunched()
        controller.onAuthFinished(AuthResult.SUCCESS)

        appLockRepository.setSettings(AppLockSettings(true, LockTimeout.IMMEDIATELY))
        controller.backgroundFor(1)

        assertTrue(controller.state.value.isLocked)
    }

    @Test
    fun turningTheLockOff_unlocksAndStopsLocking() = runTest {
        val appLockRepository = FakeAppLockRepository(AppLockSettings(true, LockTimeout.IMMEDIATELY))
        val controller = controller(appLockRepository)

        appLockRepository.setSettings(AppLockSettings(enabled = false))

        assertFalse(controller.state.value.isLocked)
        assertFalse(controller.state.value.isAppLockEnabled)
        assertFalse(controller.state.value.isPromptRequested)

        controller.backgroundFor(600_000)

        assertFalse(controller.state.value.isLocked)
    }

    @Test
    fun aLateResultWhileUnlocked_isIgnored() = runTest {
        val controller = unlockedController()

        controller.onAuthFinished(AuthResult.ERROR)

        assertFalse(controller.state.value.isLocked)
        assertNull(controller.state.value.lockMessage)
    }
}

/** Emits nothing until [emit] is called, like preferences that are still being read from disk. */
private class PendingAppLockRepository : AppLockRepository {

    private val settings = MutableSharedFlow<AppLockSettings>()

    suspend fun emit(value: AppLockSettings) = settings.emit(value)

    override fun getSettings(): Flow<AppLockSettings> = settings

    override suspend fun setSettings(settings: AppLockSettings) = Unit
}
