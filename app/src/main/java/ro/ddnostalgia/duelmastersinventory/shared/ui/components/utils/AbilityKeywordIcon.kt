package ro.ddnostalgia.duelmastersinventory.shared.ui.components.utils

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Text-glyph icon for an AbilityKeyword.iconKey (same lightweight approach as RarityIcon - no
 * drawable/vector-icon-library dependency). Placeholder glyphs, not final art; an unrecognized
 * key (a future registry row added without a matching entry here) renders nothing, which is the
 * intended safe-no-op per specs/0007.
 */
private val ICON_GLYPHS = mapOf(
    "blocker" to "⛊︎",       // ⛊ shield
    "shield_trigger" to "⚡︎", // ⚡ bolt
    "slayer" to "†︎",         // † dagger
    "civil_count" to "⬡︎",    // ⬡ hexagon
    "guard_strike" to "⛨︎",   // ⛨ black cross on shield
)

@Composable
fun AbilityKeywordIcon(iconKey: String, color: Color = MaterialTheme.colorScheme.primary) {
    val glyph = ICON_GLYPHS[iconKey] ?: return
    val size = AssistChipDefaults.IconSize
    Text(
        text = glyph,
        color = color,
        fontSize = with(LocalDensity.current) { size.toSp() * 0.8 },
        modifier = Modifier.size(size),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}
