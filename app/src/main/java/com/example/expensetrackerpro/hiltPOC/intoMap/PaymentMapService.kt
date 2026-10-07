package com.example.expensetrackerpro.hiltPOC.intoMap

import com.example.expensetrackerpro.hiltPOC.intoSet.PaymentHandler
import javax.inject.Inject

class PaymentMapService @Inject constructor(
    private val handlers:
    Map<PaymentType, @JvmSuppressWildcards PaymentHandler>
) {

    fun printHandler(type: PaymentType) {
        val handler = handlers[type]
        println("MAP -> ${handler?.name()}")
    }
}