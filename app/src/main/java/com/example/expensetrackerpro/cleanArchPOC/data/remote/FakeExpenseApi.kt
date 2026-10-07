package com.example.expensetrackerpro.cleanArchPOC.data.remote

import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject

class FakeExpenseApi @Inject constructor() : ExpenseApi {

    override suspend fun getExpenses(): List<ExpenseDto> {
        delay(1_000)
       // throw IOException("No internet")

        return listOf(
            ExpenseDto(
                id = "EXP-101",
                amount = 750.0,
                category = "FOOD",
                createdAtMillis = System.currentTimeMillis()
            ),
            ExpenseDto(
                id = "EXP-102",
                amount = 1200.0,
                category = "TRAVEL",
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }
}