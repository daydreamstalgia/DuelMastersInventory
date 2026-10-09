package ro.ddnostalgia.duelmastersinventory.shared.data.cards.setimport

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * All card art (bundled sets included - specs/0006 removed hardcoded APK assets, every
 * set including the former "bundled" catalog is a zip pack imported via Discover/onboarding)
 * lives in app-private internal storage, under `card_images/` / `card_images_low/`,
 * `{id}.jpg`-keyed. [open] returns null if a print's art hasn't been imported/downloaded.
 */
object CardImageStore {

    fun open(context: Context, relativePath: String): InputStream? {
        val file = File(context.filesDir, relativePath)
        if (!file.exists()) return null
        return try {
            file.inputStream()
        } catch (e: IOException) {
            null
        }
    }

    /** Saves [bitmap] as a JPEG at internal-storage `relativePath` (e.g. "card_images/1234.jpg"), creating parent folders as needed. */
    fun save(context: Context, relativePath: String, bitmap: Bitmap, quality: Int = 92) {
        val file = File(context.filesDir, relativePath)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
    }

    /**
     * Deletes the internal-storage copy at `relativePath`, if any - used when a dynamically
     * imported set is deleted (see decisions/0012). Never touches APK assets (can't be written
     * to at runtime, so there's nothing to delete there for a bundled print).
     */
    fun delete(context: Context, relativePath: String) {
        File(context.filesDir, relativePath).delete()
    }
}
