package com.example.expensetrackerpro.data.scan

import android.app.ActivityManager
import android.content.Context
import com.example.expensetrackerpro.domain.scan.BillScannerAvailability
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The scanner is a screen of Google Play services, which also sets how much memory it needs. */
class GmsBillScannerAvailability @Inject constructor(
    @ApplicationContext private val context: Context
) : BillScannerAvailability {

    override fun isAvailable(): Boolean {
        val playServices = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        if (playServices != ConnectionResult.SUCCESS) return false

        val memory = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memory) ?: return false

        return memory.totalMem >= MIN_TOTAL_MEMORY_BYTES
    }

    private companion object {
        // The document scanner's documented minimum: 1.7 GB of device memory
        const val MIN_TOTAL_MEMORY_BYTES = 1_700_000_000L
    }
}
