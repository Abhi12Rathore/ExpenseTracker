package com.example.expensetrackerpro.hiltPOC

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = PaymentViewModelWithParam.Factory::class)
class PaymentViewModelWithParam @AssistedInject constructor(
    private val repository: PaymentRepository,
    @Assisted val paymentId: String
) : ViewModel() {

    fun loadPayment(): String {
        return repository.fetchPayment(paymentId)
    }

    @AssistedFactory
    interface Factory {
        fun create(paymentId: String): PaymentViewModelWithParam
    }
}

//Hilt has a ViewModelComponent as part of its component hierarchy, and @HiltViewModel allows
// Hilt to integrate a particular Android ViewModel with that DI machinery.

// @AssistedFactory -> Create factory interface with create method for creating viewModel instance
// with runtime parameter -> paymentId in our case

