package com.example.expensetrackerpro.hiltPOC.intoSet

import javax.inject.Inject

class PaymentSetService @Inject constructor(
    private val handlers: Set<@JvmSuppressWildcards PaymentHandler>
) {
    fun printHandlers() {
        handlers.forEach {
            println("SET -> ${it.name()}")
        }
    }
}