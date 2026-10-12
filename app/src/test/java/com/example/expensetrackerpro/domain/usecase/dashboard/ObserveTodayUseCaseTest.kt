package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.fakes.VirtualTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveTodayUseCaseTest {

    private val oct8 = millisAt(2026, Calendar.OCTOBER, 8)
    private val oct9 = millisAt(2026, Calendar.OCTOBER, 9)
    private val oct10 = millisAt(2026, Calendar.OCTOBER, 10)

    @Test
    fun emitsTheStartOfToday_thenTheNextDayAtMidnightAndNotBefore() = runTest {
        val clock = VirtualTimeProvider(millisAt(2026, Calendar.OCTOBER, 8, hour = 23, minute = 59), testScheduler)
        val days = mutableListOf<Long>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveTodayUseCase(clock)().collect { days += it }
        }
        assertEquals(listOf(oct8), days)

        advanceTimeBy(1.minutes - 1.milliseconds)
        runCurrent()
        assertEquals(listOf(oct8), days)

        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals(listOf(oct8, oct9), days)

        advanceTimeBy(24.hours)
        runCurrent()
        assertEquals(listOf(oct8, oct9, oct10), days)
    }
}
