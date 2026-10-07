package com.example.expensetrackerpro.hiltPOC.intoSet

import javax.inject.Inject

class UpiHandler @Inject constructor() : PaymentHandler {
    override fun name(): String = "UPI"
}
