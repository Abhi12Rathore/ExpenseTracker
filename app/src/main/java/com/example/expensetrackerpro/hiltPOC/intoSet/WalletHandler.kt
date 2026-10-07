package com.example.expensetrackerpro.hiltPOC.intoSet

import javax.inject.Inject


class WalletHandler @Inject constructor() : PaymentHandler {
    override fun name(): String = "WALLET"
}