package com.example.expensetrackerpro.domain.usecase.export

import com.example.expensetrackerpro.domain.time.TimeProvider
import com.example.expensetrackerpro.utils.DateFormatter
import javax.inject.Inject

/** The name an export is offered under, dated today: `expenses_2026-10-11.csv`. */
class GetExportFileNameUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {

    operator fun invoke(): String =
        "expenses_${DateFormatter.formatIsoDate(timeProvider.now().timeInMillis)}.csv"
}
