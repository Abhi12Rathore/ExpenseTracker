package com.example.expensetrackerpro.data.export

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.ExportDestination
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FileExportRepositoryImplTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val repository = FileExportRepositoryImpl(context)

    private val destination = ExportDestination("content://com.example.documents/document/expenses.csv")

    /** Makes the document at [destination] write to [stream]. */
    private fun documentWritesTo(stream: OutputStream) {
        shadowOf(context.contentResolver).registerOutputStream(Uri.parse(destination.uri), stream)
    }

    private class FailingStream(private val error: IOException) : OutputStream() {
        var isClosed = false
            private set

        override fun write(b: Int) = throw error

        override fun close() {
            isClosed = true
        }
    }

    @Test
    fun writesTheTextAsUtf8_afterAByteOrderMark() = runTest {
        val file = ByteArrayOutputStream()
        documentWritesTo(file)
        val text = "Date,Title\r\n2026-10-11,चाय ₹20 😀\r\n"

        val result = repository.writeText(destination, text)

        assertEquals(Outcome.Success(Unit), result)
        val bytes = file.toByteArray()
        assertArrayEquals(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()), bytes.copyOf(3))
        assertEquals(text, bytes.copyOfRange(3, bytes.size).toString(Charsets.UTF_8))
    }

    @Test
    fun aDriveThatIsFull_isAFileWriteFailure() = runTest {
        val file = FailingStream(IOException("No space left on device"))
        documentWritesTo(file)

        val result = repository.writeText(destination, "Date\r\n")

        assertEquals(Outcome.Failure(DomainError.FileWriteFailed), result)
        assertTrue(file.isClosed)
    }

    @Test
    fun aDriveThatWasRemoved_isAFileWriteFailure() = runTest {
        documentWritesTo(FailingStream(FileNotFoundException("No such file or directory")))

        assertEquals(
            Outcome.Failure(DomainError.FileWriteFailed),
            repository.writeText(destination, "Date\r\n")
        )
    }

    @Test(expected = IllegalStateException::class)
    fun anythingElseThatIsThrown_isLeftToCrash() = runTest {
        documentWritesTo(object : OutputStream() {
            override fun write(b: Int) = throw IllegalStateException("a bug")
        })

        repository.writeText(destination, "Date\r\n")
    }
}
