package com.example.expensetrackerpro.hiltPOC.intoSet

import javax.inject.Inject

class CardHandler @Inject constructor() : PaymentHandler {
    override fun name(): String = "CARD"
}