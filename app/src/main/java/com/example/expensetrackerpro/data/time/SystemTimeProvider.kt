package com.example.expensetrackerpro.data.time

import com.example.expensetrackerpro.domain.time.TimeProvider
import java.util.Calendar
import javax.inject.Inject

class SystemTimeProvider @Inject constructor() : TimeProvider {

    override fun now(): Calendar = Calendar.getInstance()
}
