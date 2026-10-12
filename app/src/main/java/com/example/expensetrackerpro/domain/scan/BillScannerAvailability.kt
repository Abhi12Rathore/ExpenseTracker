package com.example.expensetrackerpro.domain.scan

interface BillScannerAvailability {

    /** Whether this device can show the scanner at all. Read fresh on every call. */
    fun isAvailable(): Boolean
}
