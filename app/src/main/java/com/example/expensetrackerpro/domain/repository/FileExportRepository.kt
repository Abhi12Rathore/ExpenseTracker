package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DocumentUri

interface FileExportRepository {

    /**
     * Writes [text] to [destination] as UTF-8, replacing what is there. Fails with
     * FileWriteFailed when the file can't be written.
     */
    suspend fun writeText(destination: DocumentUri, text: String): Outcome<Unit>
}
