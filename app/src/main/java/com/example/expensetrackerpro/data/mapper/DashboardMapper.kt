package com.example.expensetrackerpro.data.mapper

import com.example.expensetrackerpro.data.local.model.CategoryExpenseEntity
import com.example.expensetrackerpro.data.local.model.DailyCashFlowEntity
import com.example.expensetrackerpro.data.local.model.DailyExpenseEntity
import com.example.expensetrackerpro.data.local.model.MonthlyTrendEntity
import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.DailyExpense
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.MonthlyTrend

fun CategoryExpenseEntity.toDomain(): CategoryBreakdown = CategoryBreakdown(
    categoryId = categoryId,
    categoryName = categoryName,
    icon = icon,
    amount = Money(totalPaise)
)

fun DailyCashFlowEntity.toDomain(): CashFlowPoint = CashFlowPoint(
    dateMillis = dateMillis,
    income = Money(incomePaise),
    expense = Money(expensePaise)
)

fun DailyExpenseEntity.toDomain(): DailyExpense = DailyExpense(
    dateMillis = dayMillis,
    amount = Money(amountPaise),
    transactionCount = transactionCount
)

fun MonthlyTrendEntity.toDomain(): MonthlyTrend = MonthlyTrend(
    year = year,
    month = month,
    income = Money(incomePaise),
    expense = Money(expensePaise)
)
