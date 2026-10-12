package com.example.expensetrackerpro.data.scan

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.models.RecognizedText
import com.example.expensetrackerpro.domain.scan.BillTextReader
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Reads a bill with ML Kit text recognition (Latin script), which runs on the device: the photo
 * is not sent anywhere.
 */
class MlKitBillTextReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lineAssembler: BillLineAssembler
) : BillTextReader {

    // Loads the model on first use, and is kept for the scans that follow
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun read(image: DocumentUri): Outcome<RecognizedText> =
        withContext(Dispatchers.IO) {
            val uri = image.uri.toUri()

            try {
                val text = recognizer.process(InputImage.fromFilePath(context, uri)).await()
                Outcome.Success(lineAssembler.assemble(text.fragments()))
            } catch (e: IOException) {
                // The photo is gone, e.g. the cache was cleared while the app was away
                Outcome.Failure(DomainError.BillUnreadable)
            } catch (e: MlKitException) {
                // Recognition failed, or its model has not been downloaded yet
                Outcome.Failure(DomainError.BillUnreadable)
            } finally {
                delete(uri)
            }
        }

    private fun Text.fragments(): List<TextFragment> =
        textBlocks.flatMap { it.lines }.mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            TextFragment(text = line.text, left = box.left, top = box.top, height = box.height())
        }

    // The scanner saves the photo as a file in the app's cache
    private fun delete(uri: Uri) {
        if (uri.scheme == "file") uri.path?.let { File(it).delete() }
    }
}
