package com.example.expensetrackerpro.data.time

import android.content.Context
import android.text.format.DateFormat
import com.example.expensetrackerpro.domain.time.HourFormatProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SystemHourFormatProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : HourFormatProvider {

    // Read each time: the setting can be changed while the app is open
    override fun uses24Hours(): Boolean = DateFormat.is24HourFormat(context)
}
