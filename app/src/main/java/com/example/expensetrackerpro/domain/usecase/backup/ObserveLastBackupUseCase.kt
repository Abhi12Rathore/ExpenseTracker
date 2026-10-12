package com.example.expensetrackerpro.domain.usecase.backup

import com.example.expensetrackerpro.domain.models.DayLabel
import com.example.expensetrackerpro.domain.models.LastBackup
import com.example.expensetrackerpro.domain.repository.BackupInfoRepository
import com.example.expensetrackerpro.domain.time.TimeProvider
import com.example.expensetrackerpro.domain.usecase.dashboard.ObserveTodayUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject

/** When the last backup was saved, named against today, and again whenever either changes. */
class ObserveLastBackupUseCase @Inject constructor(
    private val backupInfoRepository: BackupInfoRepository,
    private val observeToday: ObserveTodayUseCase,
    private val timeProvider: TimeProvider
) {

    /** Null until a backup has been saved. */
    operator fun invoke(): Flow<LastBackup?> =
        combine(backupInfoRepository.getLastBackupMillis(), observeToday()) { millis, todayStart ->
            millis?.let { LastBackup(millis = it, day = dayOf(it, todayStart)) }
        }

    private fun dayOf(millis: Long, todayStart: Long): DayLabel {
        val yesterdayStart = timeProvider.now().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_MONTH, -1)
        }.timeInMillis

        return when {
            millis >= todayStart -> DayLabel.TODAY
            millis >= yesterdayStart -> DayLabel.YESTERDAY
            else -> DayLabel.OTHER
        }
    }
}
