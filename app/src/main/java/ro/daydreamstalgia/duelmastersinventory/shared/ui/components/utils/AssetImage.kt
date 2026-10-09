package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.IOException

@Composable
fun AssetImage(filename: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val bitmap by produceState<Bitmap?>(initialValue = null, filename) {
        value = loadBitmapFromAssets(context, filename)
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
        )
    }
}

private fun loadBitmapFromAssets(context: Context, filename: String): Bitmap? {
    return try {
        context.assets.open(filename).use { input ->
            BitmapFactory.decodeStream(input)
        }
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}