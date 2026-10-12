package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.scan

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Google's document scanner, set up for one bill: a single page as a JPEG, taken with the camera
 * or picked from the gallery. The screen belongs to Google Play services, which owns the camera,
 * so the app needs no camera permission.
 *
 * Returns what opens the scanner. [onScanned] gets the address of the photo; backing out of the
 * scanner calls nothing. [onUnavailable] is called when the scanner can't be shown at all.
 */
@Composable
fun rememberBillScanner(
    onScanned: (uri: String) -> Unit,
    onUnavailable: () -> Unit
): () -> Unit {
    val activity = LocalActivity.current
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // Anything else is the user backing out, which needs no message
        if (result.resultCode == Activity.RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                ?.pages
                ?.firstOrNull()
                ?.let { page -> currentOnScanned(page.imageUri.toString()) }
        }
    }

    return remember(activity, launcher) {
        {
            if (activity == null) {
                currentOnUnavailable()
            } else {
                GmsDocumentScanning.getClient(SCANNER_OPTIONS)
                    .getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                    // Google Play services could not provide the scanner, e.g. nothing to download it with
                    .addOnFailureListener { currentOnUnavailable() }
            }
        }
    }
}

private val SCANNER_OPTIONS = GmsDocumentScannerOptions.Builder()
    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
    .setPageLimit(1)
    .setGalleryImportAllowed(true)
    .build()
