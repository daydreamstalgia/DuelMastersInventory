package ro.ddnostalgia.duelmastersinventory.shared.utils.image

import android.graphics.Bitmap
import kotlin.math.min

/**
 * Cheap "is this basically the same picture" check for set-pack reimport conflict detection
 * (see decisions/0012-set-reimport-conflict-resolution.md) - deliberately tolerant of the JPEG
 * re-compression noise [ro.ddnostalgia.duelmastersinventory.shared.data.cards.setimport.CardImageStore.save]
 * introduces, unlike a byte-exact or [Bitmap.sameAs] comparison which would flag nearly every
 * reimport of an unchanged image as "changed".
 */
object ImageSimilarity {
    private const val HASH_SIZE = 8

    /** 8x8 grayscale average hash, packed into the low 64 bits of a [Long] (1 = above the block's mean). */
    fun averageHash(bitmap: Bitmap): Long {
        val small = Bitmap.createScaledBitmap(bitmap, HASH_SIZE, HASH_SIZE, true)
        val gray = IntArray(HASH_SIZE * HASH_SIZE)
        var sum = 0L
        for (y in 0 until HASH_SIZE) {
            for (x in 0 until HASH_SIZE) {
                val pixel = small.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val luminance = (r + g + b) / 3
                gray[y * HASH_SIZE + x] = luminance
                sum += luminance
            }
        }
        if (small != bitmap) small.recycle()

        val mean = sum / gray.size
        var hash = 0L
        for (i in gray.indices) {
            if (gray[i] >= mean) hash = hash or (1L shl i)
        }
        return hash
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    /**
     * Tuned by inspection, not measurement (no ground-truth "same card, re-encoded" corpus to
     * calibrate against) - see decisions/0012 consequences. Out of 64 bits, allows noticeable
     * JPEG blockiness/banding while still catching a genuinely different illustration.
     */
    private const val SAME_IMAGE_MAX_DISTANCE = 6

    fun looksSame(a: Bitmap, b: Bitmap): Boolean =
        hammingDistance(averageHash(a), averageHash(b)) <= min(SAME_IMAGE_MAX_DISTANCE, 64)
}
