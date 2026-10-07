package com.example.expensetrackerpro.cleanArchPOC.data.remote

interface ExpenseApi {
    suspend fun getExpenses(): List<ExpenseDto>
}