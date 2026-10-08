package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DayPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class GetDayPeriodUseCaseTest {

    private val getDayPeriod = GetDayPeriodUseCase()

    @Test
    fun midnightUntilNoon_isMorning() {
        assertEquals(DayPeriod.MORNING, getDayPeriod(0))
        assertEquals(DayPeriod.MORNING, getDayPeriod(6))
        assertEquals(DayPeriod.MORNING, getDayPeriod(11))
    }

    @Test
    fun noonUntilFive_isAfternoon() {
        assertEquals(DayPeriod.AFTERNOON, getDayPeriod(12))
        assertEquals(DayPeriod.AFTERNOON, getDayPeriod(16))
    }

    @Test
    fun fiveOnwards_isEvening() {
        assertEquals(DayPeriod.EVENING, getDayPeriod(17))
        assertEquals(DayPeriod.EVENING, getDayPeriod(23))
    }
}
