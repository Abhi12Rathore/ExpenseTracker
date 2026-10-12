package com.example.expensetrackerpro.data.files

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.example.expensetrackerpro.domain.models.DocumentUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * Reads and writes the documents the user picks in the system file screens, through
 * `ContentResolver`. Every call blocks, so it belongs on an IO dispatcher, and throws
 * [IOException] when the document can't be reached, e.g. its drive is full or was removed.
 */
class DocumentFiles @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Replaces what the document holds with [bytes]. */
    @Throws(IOException::class)
    fun write(document: DocumentUri, bytes: ByteArray) {
        // "wt" empties a file that is being replaced, so nothing of the old one is left behind
        val stream = context.contentResolver.openOutputStream(document.uri.toUri(), "wt")
            ?: throw IOException("No output stream for the document")

        stream.use { it.write(bytes) }
    }

    /**
     * The document as UTF-8 text, without a byte order mark if it starts with one. Null when
     * it is longer than [maxBytes], which is found out without reading the rest of it.
     */
    @Throws(IOException::class)
    fun readText(document: DocumentUri, maxBytes: Int): String? {
        val stream = context.contentResolver.openInputStream(document.uri.toUri())
            ?: throw IOException("No input stream for the document")

        val bytes = stream.use { input ->
            val collected = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                collected.write(buffer, 0, read)
                if (collected.size() > maxBytes) return null
            }
            collected.toByteArray()
        }

        return bytes.toString(Charsets.UTF_8).removePrefix(BYTE_ORDER_MARK)
    }

    /** The name the document is listed under, e.g. `expenses.csv`, or null when its provider gives none. */
    fun displayName(document: DocumentUri): String? =
        context.contentResolver
            .query(document.uri.toUri(), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    private companion object {
        const val BUFFER_SIZE = 8 * 1024
        // Written as its code so that the character itself is not in this file
        val BYTE_ORDER_MARK = 0xFEFF.toChar().toString()
    }
}
