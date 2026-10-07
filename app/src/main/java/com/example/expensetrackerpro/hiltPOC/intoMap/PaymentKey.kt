package com.example.expensetrackerpro.hiltPOC.intoMap

import dagger.MapKey

@MapKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PaymentKey(
    val value: PaymentType
)