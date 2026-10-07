package com.example.expensetrackerpro.hiltPOC

import javax.inject.Inject

class PaymentApi @Inject constructor() {
    fun getPayment(paymentId:String?): String {
        return "Payment received $paymentId"
    }
}