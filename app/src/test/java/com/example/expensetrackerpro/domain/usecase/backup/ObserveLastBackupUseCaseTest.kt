package com.example.expensetrackerpro.domain.usecase.backup

import com.example.expensetrackerpro.domain.models.DayLabel
import com.example.expensetrackerpro.domain.models.LastBackup
import com.example.expensetrackerpro.domain.usecase.dashboard.ObserveTodayUseCase
import com.example.expensetrackerpro.fakes.FakeBackupInfoRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.VirtualTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveLastBackupUseCaseTest {

    // Sunday 11 Oct 2026, 4:20 pm
    private val time = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 11, hour = 16, minute = 20))

    private suspend fun lastBackupAt(millis: Long?): LastBackup? =
        ObserveLastBackupUseCase(FakeBackupInfoRepository(millis), ObserveTodayUseCase(time), time)().first()

    @Test
    fun noBackupYet_isNull() = runTest {
        assertNull(lastBackupAt(null))
    }

    @Test
    fun aBackupMadeToday_isToday_fromTheFirstMomentOfTheDay() = runTest {
        val justNow = time.nowMillis
        val midnight = millisAt(2026, Calendar.OCTOBER, 11)

        assertEquals(LastBackup(justNow, DayLabel.TODAY), lastBackupAt(justNow))
        assertEquals(LastBackup(midnight, DayLabel.TODAY), lastBackupAt(midnight))
    }

    @Test
    fun aBackupMadeTheDayBefore_isYesterday() = runTest {
        val lastMoment = millisAt(2026, Calendar.OCTOBER, 11) - 1
        val firstMoment = millisAt(2026, Calendar.OCTOBER, 10)

        assertEquals(DayLabel.YESTERDAY, lastBackupAt(lastMoment)?.day)
        assertEquals(DayLabel.YESTERDAY, lastBackupAt(firstMoment)?.day)
    }

    @Test
    fun anOlderBackup_isJustItsDate() = runTest {
        assertEquals(DayLabel.OTHER, lastBackupAt(millisAt(2026, Calendar.OCTOBER, 10) - 1)?.day)
        assertEquals(DayLabel.OTHER, lastBackupAt(millisAt(2025, Calendar.MARCH, 3))?.day)
    }

    @Test
    fun followsANewBackup_andTheDayChangingAtMidnight() = runTest {
        val start = millisAt(2026, Calendar.OCTOBER, 11, hour = 23, minute = 59)
        val clock = VirtualTimeProvider(start, testScheduler)
        val backupInfo = FakeBackupInfoRepository()
        val seen = mutableListOf<LastBackup?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveLastBackupUseCase(backupInfo, ObserveTodayUseCase(clock), clock)().collect { seen += it }
        }
        assertEquals(listOf<LastBackup?>(null), seen)

        backupInfo.setLastBackupMillis(start)
        assertEquals(LastBackup(start, DayLabel.TODAY), seen.last())

        advanceTimeBy(1.minutes)
        runCurrent()
        assertEquals(LastBackup(start, DayLabel.YESTERDAY), seen.last())
    }
}
