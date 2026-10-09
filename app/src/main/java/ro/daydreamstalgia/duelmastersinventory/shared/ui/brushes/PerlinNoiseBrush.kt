package ro.daydreamstalgia.duelmastersinventory.shared.ui.brushes

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import kotlin.math.floor

class PerlinNoiseBrush(
    colors: List<Color>,
    width: Int = 128,
    height: Int = 128
) : ShaderBrush() {

    private val bitmap = generatePerlinNoiseBitmap(width, height, colors)

    override fun createShader(size: Size): Shader {
        return ImageShader(
            image = bitmap.asImageBitmap(),
            tileModeX = TileMode.Repeated,
            tileModeY = TileMode.Repeated
        )
    }
}

fun generatePerlinNoiseBitmap(
    width: Int,
    height: Int,
    colors: List<Color>,
    scale: Float = 0.1f,   // controls zoom level of noise
    octaves: Int = 4,
    persistence: Float = 1.5f
): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    fun fade(t: Float) = t * t * t * (t * (t * 6 - 15) + 10)
    fun lerp(a: Float, b: Float, t: Float) = a + t * (b - a)
    fun grad(hash: Int, x: Float, y: Float): Float {
        val h = hash and 7
        val u = if (h < 4) x else y
        val v = if (h < 4) y else x
        return if ((h and 1) == 0) u else -u + if ((h and 2) == 0) v else -v
    }

    val perm = IntArray(512) { it % 256 }
    perm.shuffle()

    fun noise(x: Float, y: Float): Float {
        val xi = floor(x).toInt() and 255
        val yi = floor(y).toInt() and 255
        val xf = x - floor(x)
        val yf = y - floor(y)

        val u = fade(xf)
        val v = fade(yf)

        val aa = perm[perm[xi] + yi]
        val ab = perm[perm[xi] + yi + 1]
        val ba = perm[perm[xi + 1] + yi]
        val bb = perm[perm[xi + 1] + yi + 1]

        val x1 = lerp(grad(aa, xf, yf), grad(ba, xf - 1, yf), u)
        val x2 = lerp(grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1), u)
        return (lerp(x1, x2, v) + 1) / 2f // normalize to 0..1
    }

    fun fractalNoise(x: Float, y: Float): Float {
        var total = 0f
        var frequency = scale
        var amplitude = 1f
        var maxValue = 0f
        repeat(octaves) {
            total += noise(x * frequency, y * frequency) * amplitude
            maxValue += amplitude
            amplitude *= persistence
            frequency *= 2
        }
        return total / maxValue
    }

    // Fill bitmap
    for (y in 0 until height) {
        for (x in 0 until width) {
            val n = fractalNoise(x.toFloat(), y.toFloat())
            // map noise to gradient colors
            val idx = (n * (colors.size - 1)).toInt().coerceIn(0, colors.size - 2)
            val t = n * (colors.size - 1) - idx
            val c1 = colors[idx]
            val c2 = colors[idx + 1]
            val r = lerp(c1.red, c2.red, t)
            val g = lerp(c1.green, c2.green, t)
            val b = lerp(c1.blue, c2.blue, t)
            val a = lerp(c1.alpha, c2.alpha, t)
            bmp.setPixel(x, y, Color(
                (r * 255).toInt(),
                (g * 255).toInt(),
                (b * 255).toInt(),
                (a * 255).toInt(),
            ).toArgb())
        }
    }

    return bmp
}