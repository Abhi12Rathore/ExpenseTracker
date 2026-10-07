package com.example.expensetrackerpro.domain.models

import com.example.expensetrackerpro.utils.InsightPriority
import com.example.expensetrackerpro.utils.InsightType

data class SmartInsight(
    val title: String,
    val description: String,
    val type: InsightType,
    val priority: InsightPriority
)