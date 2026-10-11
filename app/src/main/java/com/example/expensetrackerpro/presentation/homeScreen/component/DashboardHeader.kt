package com.example.expensetrackerpro.presentation.homeScreen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.DayPeriod
import com.example.expensetrackerpro.domain.models.UserProfile
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.presentation.common.profile.ProfileAvatar
import com.example.expensetrackerpro.utils.DateFormatter

@Composable
fun DashboardHeader(profile: UserProfile, dayPeriod: DayPeriod?, todayMillis: Long?) {
    // Blank for the moment before the dashboard has loaded, so the lines keep their height and
    // "Welcome" never shows ahead of a saved name
    val greeting = when (dayPeriod) {
        DayPeriod.MORNING -> stringResource(R.string.greeting_morning)
        DayPeriod.AFTERNOON -> stringResource(R.string.greeting_afternoon)
        DayPeriod.EVENING -> stringResource(R.string.greeting_evening)
        null -> ""
    }
    val name = when {
        dayPeriod == null -> ""
        // Someone who has not given a name is still greeted
        else -> profile.name.ifBlank { stringResource(R.string.greeting_no_name) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            DateChip(todayMillis = todayMillis)
        }
        // Decorative: the name is already read out beside it
        ProfileAvatar(
            initial = profile.initial,
            size = 48.dp,
            textStyle = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun DateChip(todayMillis: Long?) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        // Kept in the layout but unseen until the date is known, so the header doesn't jump
        modifier = Modifier.alpha(if (todayMillis == null) 0f else 1f)
    ) {
        Text(
            text = todayMillis?.let(DateFormatter::formatWeekdayDayMonth).orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@PreviewLightDark
@Composable
private fun DashboardHeaderPreview() {
    ExpenseTrackerProTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                DashboardHeader(
                    profile = UserProfile(name = "Abhishek"),
                    dayPeriod = DayPeriod.MORNING,
                    todayMillis = PreviewData.DATE_MILLIS
                )
                DashboardHeader(
                    profile = UserProfile(),
                    dayPeriod = DayPeriod.AFTERNOON,
                    todayMillis = PreviewData.DATE_MILLIS
                )
                DashboardHeader(
                    profile = UserProfile(name = "Abhishek Pratap Singh Rathore the Third"),
                    dayPeriod = DayPeriod.EVENING,
                    todayMillis = PreviewData.DATE_MILLIS
                )
            }
        }
    }
}
