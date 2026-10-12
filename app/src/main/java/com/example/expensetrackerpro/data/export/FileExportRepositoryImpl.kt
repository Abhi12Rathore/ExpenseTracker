package com.example.expensetrackerpro.data.export

import com.example.expensetrackerpro.data.files.DocumentFiles
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.repository.FileExportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class FileExportRepositoryImpl @Inject constructor(
    private val documentFiles: DocumentFiles
) : FileExportRepository {

    override suspend fun writeText(destination: DocumentUri, text: String): Outcome<Unit> =
        withContext(Dispatchers.IO) {
            // A full or removed drive is expected. Anything else that is thrown is a bug and carries on up.
            try {
                documentFiles.write(destination, UTF8_BOM + text.toByteArray(Charsets.UTF_8))
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
