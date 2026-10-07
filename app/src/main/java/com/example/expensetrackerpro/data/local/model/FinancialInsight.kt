package com.example.expensetrackerpro.data.local.model

import com.example.expensetrackerpro.utils.InsightType

data class FinancialInsight(

    val title: String,

    val description: String,

    val type: InsightType

)