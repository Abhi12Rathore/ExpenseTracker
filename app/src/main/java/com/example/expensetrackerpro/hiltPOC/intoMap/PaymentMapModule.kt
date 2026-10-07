package com.example.expensetrackerpro.hiltPOC.intoMap

import com.example.expensetrackerpro.hiltPOC.intoSet.CardHandler
import com.example.expensetrackerpro.hiltPOC.intoSet.PaymentHandler
import com.example.expensetrackerpro.hiltPOC.intoSet.UpiHandler
import com.example.expensetrackerpro.hiltPOC.intoSet.WalletHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
object PaymentMapModule {

    @Provides
    @IntoMap
    @PaymentKey(PaymentType.UPI)
    fun provideUpiHandler(
        handler: UpiHandler
    ): PaymentHandler = handler

    @Provides
    @IntoMap
    @PaymentKey(PaymentType.CARD)
    fun provideCardHandler(
        handler: CardHandler
    ): PaymentHandler = handler

    @Provides
    @IntoMap
    @PaymentKey(PaymentType.WALLET)
    fun provideWalletHandler(
        handler: WalletHandler
    ): PaymentHandler = handler
}