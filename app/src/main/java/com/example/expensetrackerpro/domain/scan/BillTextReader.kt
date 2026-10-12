package com.example.expensetrackerpro.domain.scan

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.models.RecognizedText

interface BillTextReader {

    /**
     * The text in the photo at [image], which may be none at all. The photo is deleted once it
     * has been read, whether or not that worked: a bill is never kept.
     */
    suspend fun read(image: DocumentUri): Outcome<RecognizedText>
}
