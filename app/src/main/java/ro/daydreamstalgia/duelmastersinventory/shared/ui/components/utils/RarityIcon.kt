package ro.daydreamstalgia.duelmastersinventory.shared.ui.components.utils

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

@Composable
fun RarityIcon(
    rarity:String?,
    color: Color = MaterialTheme.colorScheme.inverseSurface) {
    fun getRaritySymbol():String?{
        if(rarity=="Common") return  "●"
        if(rarity=="Uncommon") return "♦\uFE0E" // force text glyph
        if(rarity=="Rare") return "★"
        if(rarity=="Very Rare") return "✪"
        if(rarity=="Super Rare") return "✜"
        return null
    }

    val raritySymbol = getRaritySymbol()
    val size = AssistChipDefaults.IconSize

    if(raritySymbol!=null) {
        Text(text = raritySymbol,
            color = color,
            fontSize = with(LocalDensity.current) { size.toSp() * 0.7 },
            modifier = Modifier.size(size),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}