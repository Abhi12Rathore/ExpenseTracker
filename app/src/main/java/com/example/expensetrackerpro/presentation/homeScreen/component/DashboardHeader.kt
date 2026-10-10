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
fun DashboardHeader(userName: String, dayPeriod: DayPeriod?) {
    // Someone who has not given a name is still greeted
    val name = userName.ifBlank { stringResource(R.string.greeting_default_name) }
    // Blank for the moment before the time of day is known, so the line keeps its height
    val greeting = when (dayPeriod) {
        DayPeriod.MORNING -> stringResource(R.string.greeting_morning, name)
        DayPeriod.AFTERNOON -> stringResource(R.string.greeting_afternoon, name)
        DayPeriod.EVENING -> stringResource(R.string.greeting_evening, name)
        null -> ""
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = greeting,
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