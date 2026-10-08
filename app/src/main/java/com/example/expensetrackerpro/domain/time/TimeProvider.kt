package com.example.expensetrackerpro.domain.time

import java.util.Calendar

interface TimeProvider {

    /** A new calendar set to the current instant, carrying the time zone and first day of the week. */
    fun now(): Calendar
}
