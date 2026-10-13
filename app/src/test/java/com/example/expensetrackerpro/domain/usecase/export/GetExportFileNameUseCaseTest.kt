package com.example.expensetrackerpro.domain.usecase.export

import com.example.expensetrackerpro.fakes.dateFormatter
import com.example.expensetrackerpro.domain.models.ExportFile
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.KOLKATA
import com.example.expensetrackerpro.fakes.millisAt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class GetExportFileNameUseCaseTest {

    private val originalTimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        TimeZone.setDefault(KOLKATA)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun isNamedAfterToday() {
        val time = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 11, hour = 16, minute = 20))

        assertEquals("expenses_2026-10-11.csv", GetExportFileNameUseCase(time, dateFormatter())(ExportFile.TRANSACTIONS_CSV))
    }

    @Test
    fun aBackup_hasItsOwnNameAndExtension() {
        val time = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 11, hour = 16, minute = 20))

        assertEquals("expensetracker-backup-2026-10-11.json", GetExportFileNameUseCase(time, dateFormatter())(ExportFile.BACKUP))
    }

    @Test
    fun padsTheMonthAndDay() {
        val time = FakeTimeProvider(millisAt(2027, Calendar.JANUARY, 3))

        assertEquals("expenses_2027-01-03.csv", GetExportFileNameUseCase(time, dateFormatter())(ExportFile.TRANSACTIONS_CSV))
    }
}
