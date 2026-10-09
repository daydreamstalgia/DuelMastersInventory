package ro.daydreamstalgia.duelmastersinventory.shared.ui.brushes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import ro.daydreamstalgia.duelmastersinventory.shared.utils.types.Civilization

class CivilizationBrush(
    civilization: String?,
    bgColor: Color,
) : ShaderBrush() {

    // Base/edge stops stay at the screen background; the civilization stop(s) in between are
    // blended less aggressively toward it (spec `2b`'s radial glow reads as a genuinely
    // colored ring, not a washed-out tint).
    private val colors = listOf(bgColor) +
        Civilization.fromCombo(civilization).map { Color(ColorUtils.blendARGB(it.color.toArgb(), bgColor.toArgb(), 0.35F)) } +
        listOf(bgColor)

    private val radialBrush = object : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            val biggerDimension = maxOf(size.height, size.width)
            return RadialGradientShader(
                colors = colors,
                center = size.center + Offset(0f, size.height / 2),
                radius = biggerDimension / 1.5f,
                colorStops = List(colors.size) {
                    i -> i.toFloat() / colors.size
                }.map { 0.2f + 0.4f * it }
            )
        }
    }

    override fun createShader(size: Size): Shader {
        return radialBrush.createShader(size)
    }
}
