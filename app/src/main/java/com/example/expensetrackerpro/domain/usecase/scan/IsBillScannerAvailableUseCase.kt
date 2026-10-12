package com.example.expensetrackerpro.domain.usecase.scan

import com.example.expensetrackerpro.domain.scan.BillScannerAvailability
import javax.inject.Inject

class IsBillScannerAvailableUseCase @Inject constructor(
    private val billScannerAvailability: BillScannerAvailability
) {

    operator fun invoke(): Boolean = billScannerAvailability.isAvailable()
}
