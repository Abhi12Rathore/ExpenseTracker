package com.example.expensetrackerpro.hiltPOC

import javax.inject.Inject

class PaymentRepository @Inject constructor(private val api: PaymentApi) {
    fun fetchPayment(paymentId:String?): String {
        return api.getPayment(paymentId)
    }
}


