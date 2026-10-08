package com.example.expensetrackerpro.domain.models

import com.example.expensetrackerpro.utils.InsightType

sealed interface SmartInsight {

    val type: InsightType

    data class Saved(val amount: Double) : SmartInsight {
        override val type = InsightType.POSITIVE
    }

    data class HighestSpending(val categoryName: String) : SmartInsight {
        override val type = InsightType.WARNING
    }

    data object SpendingImproved : SmartInsight {
        override val type = InsightType.POSITIVE
    }

    data object SpendingIncreased : SmartInsight {
        override val type = InsightType.WARNING
    }
}
