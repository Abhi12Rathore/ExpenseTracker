package com.example.expensetrackerpro.hiltPOC

import androidx.lifecycle.ViewModel
import com.example.expensetrackerpro.hiltPOC.intoMap.PaymentMapService
import com.example.expensetrackerpro.hiltPOC.intoMap.PaymentType
import com.example.expensetrackerpro.hiltPOC.intoSet.PaymentSetService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val repository: PaymentRepository,
    private val setService: PaymentSetService,
    private val mapService: PaymentMapService
) : ViewModel() {

    fun loadPayment(): String {
        return repository.fetchPayment(null)
    }

    fun testMultibinding() {
        setService.printHandlers()

        mapService.printHandler(PaymentType.UPI)
        mapService.printHandler(PaymentType.CARD)
    }
}

//@Inject       → constructor injection
//@Provides     → explicit creation recipe
//@Binds        → interface → implementation
//@Qualifier    → distinguish same-type bindings
//@Singleton    → lifetime/reuse of a binding
//@InstallIn    → component where binding lives


//Hilt analyzes the dependency graph at compile time and generates the code required for dependency
//resolution. Therefore, errors such as missing bindings or duplicate bindings can be detected durin
//compilation. At runtime, Hilt creates dependency instances when they are actually requested, and their
// reuse/lifetime is controlled by their scopes.


//“Because @EntryPoint is a manual escape hatch into the Hilt graph. Whenever Hilt directly supports the
// consumer, normal constructor/field injection through @AndroidEntryPoint is preferable because
// dependencies stay explicit and the design avoids service-locator style access.”