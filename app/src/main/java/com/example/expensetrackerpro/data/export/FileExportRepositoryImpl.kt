package com.example.expensetrackerpro.data.export

import android.content.Context
import androidx.core.net.toUri
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.ExportDestination
import com.example.expensetrackerpro.domain.repository.FileExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class FileExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileExportRepository {

    override suspend fun writeText(destination: ExportDestination, text: String): Outcome<Unit> =
        withContext(Dispatchers.IO) {
            // A full or removed drive is expected. Anything else that is thrown is a bug and carries on up.
            try {
                // "wt" empties a file that is being replaced, so nothing of the old one is left behind
                val stream = context.contentResolver.openOutputStream(destination.uri.toUri(), "wt")
                    ?: return@withContext Outcome.Failure(DomainError.FileWriteFailed)

                stream.use {
                    it.write(UTF8_BOM)
                    it.write(text.toByteArray(Charsets.UTF_8))
                }
                Outcome.Success(Unit)
            } catch (e: IOException) {
                Outcome.Failure(DomainError.FileWriteFailed)
            }
        }

    private companion object {
        // Tells Excel the file is UTF-8, so ₹ and non-Latin names are read correctly
        val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }
}
