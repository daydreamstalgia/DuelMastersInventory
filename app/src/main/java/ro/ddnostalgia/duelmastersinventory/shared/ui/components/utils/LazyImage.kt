package ro.ddnostalgia.duelmastersinventory.shared.ui.components.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ro.ddnostalgia.duelmastersinventory.R
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.setimport.CardImageStore
import java.io.IOException

/**
 * Decoded-bitmap cache shared by every [LazyImage], keyed on asset path (which already
 * includes the resolution folder, e.g. "card_images_low/123.jpg" vs "card_images/123.jpg"),
 * so each resolution tier is cached independently and scrolling doesn't re-decode JPEGs
 * that are already on screen.
 */
private object LazyImageCache {
    private val maxSizeKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    val bitmaps = object : LruCache<String, Bitmap>(maxSizeKb) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
}

/**
 * [filename] is read via [CardImageStore] (internal storage - no bundled-asset fallback
 * since specs/0006 removed all bundled catalog art). A print with no stored art (not
 * downloaded/imported yet) shows `ic_card_placeholder` (drawable-nodpi/ic_card_placeholder.png) -
 * the per-pixel variance across every card art image on this device, normalized per RGB
 * channel to the full 0-255 range - instead of blank gray.
 */
@Composable
fun LazyImage(
    filename: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = LazyImageCache.bitmaps.get(filename), filename) {
        if (value != null) return@produceState

        // Decoding off the main thread isn't just for perf: withContext is this coroutine's
        // only suspension point, so it's also what lets this producer actually get cancelled
        // when [filename] changes again before it finishes. Without one, a slow typist in the
        // scan screen's no-match search (which re-filters, and remounts every result row, on
        // every keystroke) can pile up dozens of these decodes as un-cancellable synchronous
        // work competing with recomposition on the same main thread - so a row can go on
        // showing an earlier keystroke's now-wrong bitmap well after its own text has already
        // updated to the right print, simply because its own decode is still stuck in that
        // backlog.
        val decoded = withContext(Dispatchers.IO) {
            try {
                CardImageStore.open(context, filename)?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (e: IOException) {
                null
            }
        }
        value = decoded
        decoded?.let { LazyImageCache.bitmaps.put(filename, it) }
    }

    Box(
        modifier = modifier
            .aspectRatio(0.7f) // e.g., card shape — adjust as needed
            .background(Color.LightGray), // placeholder bg
        contentAlignment = Alignment.Center,
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_card_placeholder),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
