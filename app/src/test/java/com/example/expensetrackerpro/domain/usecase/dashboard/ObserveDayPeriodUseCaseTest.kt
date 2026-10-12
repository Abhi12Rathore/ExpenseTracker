package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DayPeriod
import com.example.expensetrackerpro.fakes.VirtualTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveDayPeriodUseCaseTest {

    /** The periods emitted from [hour]:[minute] on Thursday 8 Oct 2026, as virtual time moves on. */
    private fun TestScope.periodsFrom(hour: Int, minute: Int = 0): List<DayPeriod> {
        val clock = VirtualTimeProvider(millisAt(2026, Calendar.OCTOBER, 8, hour, minute), testScheduler)
        val emitted = mutableListOf<DayPeriod>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveDayPeriodUseCase(clock, GetDayPeriodUseCase())().collect { emitted += it }
        }
        return emitted
    }

    private fun TestScope.wait(duration: Duration) {
        advanceTimeBy(duration)
        runCurrent()
    }

    @Test
    fun startsWithThePeriodOfTheCurrentHour() = runTest {
        suspend fun firstAt(hour: Int): DayPeriod {
            val clock = VirtualTimeProvider(millisAt(2026, Calendar.OCTOBER, 8, hour) - testScheduler.currentTime, testScheduler)
            return ObserveDayPeriodUseCase(clock, GetDayPeriodUseCase())().first()
        }

        assertEquals(DayPeriod.EVENING, firstAt(2))
        assertEquals(DayPeriod.MORNING, firstAt(5))
        assertEquals(DayPeriod.MORNING, firstAt(8))
        assertEquals(DayPeriod.AFTERNOON, firstAt(12))
        assertEquals(DayPeriod.EVENING, firstAt(17))
    }

    @Test
    fun morningBecomesAfternoon_atNoonAndNotBefore() = runTest {
        val periods = periodsFrom(hour = 11, minute = 59)

        wait(1.minutes - 1.milliseconds)
        assertEquals(listOf(DayPeriod.MORNING), periods)

        wait(1.milliseconds)
        assertEquals(listOf(DayPeriod.MORNING, DayPeriod.AFTERNOON), periods)

        wait(1.seconds)
        assertEquals(listOf(DayPeriod.MORNING, DayPeriod.AFTERNOON), periods)
    }

    @Test
    fun afternoonBecomesEvening_atFiveAndNotBefore() = runTest {
        val periods = periodsFrom(hour = 16, minute = 59)

        wait(1.minutes - 1.milliseconds)
        assertEquals(listOf(DayPeriod.AFTERNOON), periods)

        wait(1.milliseconds)
        assertEquals(listOf(DayPeriod.AFTERNOON, DayPeriod.EVENING), periods)
    }

    @Test
    fun eveningRunsAcrossMidnight_andBecomesMorningAtFive() = runTest {
        val periods = periodsFrom(hour = 23)

        // 04:59:59.999 the next day
        wait(6.hours - 1.milliseconds)
        assertEquals(listOf(DayPeriod.EVENING), periods)

        wait(1.milliseconds)
        assertEquals(listOf(DayPeriod.EVENING, DayPeriod.MORNING), periods)
    }

    @Test
    fun aWholeDay_emitsEachPeriodOnce_andComesBackToMorning() = runTest {
        val periods = periodsFrom(hour = 8)

        wait(24.hours)

        assertEquals(
            listOf(DayPeriod.MORNING, DayPeriod.AFTERNOON, DayPeriod.EVENING, DayPeriod.MORNING),
            periods
        )
    }
}
