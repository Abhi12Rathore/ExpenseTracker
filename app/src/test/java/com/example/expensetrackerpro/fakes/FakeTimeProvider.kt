package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.time.TimeProvider
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

val KOLKATA: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

/** Epoch millis of a wall-clock time in [timeZone]. [month] is a [Calendar] month constant. */
fun millisAt(
    year: Int,
    month: Int,
    day: Int,
    hour: Int = 0,
    minute: Int = 0,
    timeZone: TimeZone = KOLKATA
): Long = GregorianCalendar(timeZone, Locale.US).apply {
    clear()
    set(year, month, day, hour, minute)
}.timeInMillis

/** A clock stopped at [nowMillis] until a test moves it, independent of the machine's zone and locale. */
class FakeTimeProvider(
    var nowMillis: Long,
    private val timeZone: TimeZone = KOLKATA,
    private val firstDayOfWeek: Int = Calendar.SUNDAY
) : TimeProvider {

    override fun now(): Calendar = GregorianCalendar(timeZone, Locale.US).also {
        it.firstDayOfWeek = firstDayOfWeek
        it.timeInMillis = nowMillis
    }
}

/** A clock that starts at [startMillis] and runs with [scheduler]'s virtual time, so a delay moves it on. */
class VirtualTimeProvider(
    private val startMillis: Long,
    private val scheduler: TestCoroutineScheduler,
    private val timeZone: TimeZone = KOLKATA
) : TimeProvider {

    override fun now(): Calendar = GregorianCalendar(timeZone, Locale.US).also {
        it.timeInMillis = startMillis + scheduler.currentTime
    }
}
