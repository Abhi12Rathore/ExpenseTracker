package com.example.expensetrackerpro.presentation.homeScreen.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.DayPeriod

@Composable
fun DashboardHeader(userName: String, dayPeriod: DayPeriod) {
    val greeting = when (dayPeriod) {
        DayPeriod.MORNING -> R.string.greeting_morning
        DayPeriod.AFTERNOON -> R.string.greeting_afternoon
        DayPeriod.EVENING -> R.string.greeting_evening
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(greeting, userName),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.dashboard_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}