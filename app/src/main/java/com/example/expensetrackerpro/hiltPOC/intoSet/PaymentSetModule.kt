package com.example.expensetrackerpro.hiltPOC.intoSet

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object PaymentSetModule {

    @Provides
    @IntoSet
    fun provideUpiHandler(
        handler: UpiHandler
    ): PaymentHandler = handler

    @Provides
    @IntoSet
    fun provideCardHandler(
        handler: CardHandler
    ): PaymentHandler = handler

    @Provides
    @IntoSet
    fun provideWalletHandler(
        handler: WalletHandler
    ): PaymentHandler = handler
}