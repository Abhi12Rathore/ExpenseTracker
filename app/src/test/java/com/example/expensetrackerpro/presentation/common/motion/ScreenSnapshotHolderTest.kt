package com.example.expensetrackerpro.presentation.common.motion

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ScreenSnapshotHolderTest {

    private val time = FakeTimeProvider(nowMillis = 5_000_000L)
    private val holder = ScreenSnapshotHolder(time)

    @Test
    fun thePictureIsHandedOutOnce() {
        val picture = FakeImageBitmap()
        holder.put(picture)

        assertSame(picture, holder.take())
        assertNull(holder.take())
    }

    @Test
    fun aPictureNobodyTookInTime_isLetGo() {
        holder.put(FakeImageBitmap())

        time.nowMillis += ScreenSnapshotHolder.MAX_AGE_MILLIS + 1

        assertNull(holder.take())
    }

    @Test
    fun aNewPicture_replacesTheOneBefore() {
        holder.put(FakeImageBitmap())
        val newer = FakeImageBitmap()
        holder.put(newer)

        assertSame(newer, holder.take())
    }

    @Test
    fun withNothingPut_thereIsNothingToTake() {
        assertNull(holder.take())
    }

    private class FakeImageBitmap : ImageBitmap {
        override val width = 1
        override val height = 1
        override val config = ImageBitmapConfig.Argb8888
        override val colorSpace: ColorSpace = ColorSpaces.Srgb
        override val hasAlpha = true

        override fun prepareToDraw() = Unit

        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int
        ) = Unit
    }
}
