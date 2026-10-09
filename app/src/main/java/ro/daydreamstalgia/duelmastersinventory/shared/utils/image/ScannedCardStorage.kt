package ro.daydreamstalgia.duelmastersinventory.shared.utils.image

import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Persists the cropped capture that entered the ORB match pipeline, alongside the print identity
 * and condition it was confirmed under - a debug audit trail for offline match tuning (see
 * decisions/0009), not something the app reads back itself. Written to public external storage
 * (not app-scoped) at a fixed path so it can be pulled off-device.
 */
object ScannedCardStorage {
    private const val TAG = "ScannedCardStorage"

    private fun scannedDir(): File =
        File(Environment.getExternalStorageDirectory(), "ddnostalgia/DuelMastersInventory/scanned")

    /** [printIdentity] is `{lang}_{set_name}_{set_number}_{set_count}`, see [ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrint.displayId]. */
    fun save(bitmap: Bitmap, printIdentity: String, condition: String?) {
        try {
            val dir = scannedDir()
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "Could not create $dir")
                return
            }
            val timestamp = System.currentTimeMillis()
            FileOutputStream(File(dir, "$timestamp.png")).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val json = JSONObject().apply {
                put("print", printIdentity)
                put("condition", condition ?: JSONObject.NULL)
            }
            File(dir, "$timestamp.json").writeText(json.toString(2))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist scanned capture", e)
        }
    }
}
