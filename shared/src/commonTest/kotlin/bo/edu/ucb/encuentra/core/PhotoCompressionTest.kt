package bo.edu.ucb.encuentra.core

import bo.edu.ucb.encuentra.core.util.PhotoCompression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PhotoCompressionTest {

    @Test
    fun largePhotosAreReducedKeepingTheProportion() {
        // Foto de cámara de 12 MP (4000x3000) → lado mayor 1600.
        assertEquals(1600 to 1200, PhotoCompression.targetSize(4000, 3000))
        // Vertical.
        assertEquals(1200 to 1600, PhotoCompression.targetSize(3000, 4000))
    }

    @Test
    fun smallPhotosAreNotEnlarged() {
        assertEquals(1280 to 720, PhotoCompression.targetSize(1280, 720))
        assertEquals(1600 to 900, PhotoCompression.targetSize(1600, 900))
    }

    @Test
    fun sampleSizeDecodesCloseToTheTargetWithoutGoingBelow() {
        // 4000 / 2 = 2000 ≥ 1600; 4000 / 4 = 1000 < 1600 → 2.
        assertEquals(2, PhotoCompression.sampleSize(4000, 3000))
        assertEquals(4, PhotoCompression.sampleSize(8000, 6000))
        assertEquals(1, PhotoCompression.sampleSize(1600, 1200))
    }

    @Test
    fun invalidSizesFail() {
        assertFailsWith<IllegalArgumentException> { PhotoCompression.targetSize(0, 100) }
    }
}
