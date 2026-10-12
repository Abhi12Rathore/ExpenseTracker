package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.repository.FileExportRepository
import kotlinx.coroutines.CompletableDeferred

/** Keeps what it was asked to write instead of touching a file. */
class FakeFileExportRepository : FileExportRepository {

    /** Every write that went through, oldest first: where to, and the text. */
    val written = mutableListOf<Pair<DocumentUri, String>>()

    /** How many times a write was asked for, whether or not it went through. */
    var writeCalls = 0
        private set

    /** While true every write fails, as a full or removed drive would make it. */
    var fail = false

    /** When set, every write waits for it to complete, as a slow drive would. */
    var writeGate: CompletableDeferred<Unit>? = null

    override suspend fun writeText(destination: DocumentUri, text: String): Outcome<Unit> {
        writeCalls++
        writeGate?.await()
        if (fail) return Outcome.Failure(DomainError.FileWriteFailed)
        written += destination to text
        return Outcome.Success(Unit)
    }
}
