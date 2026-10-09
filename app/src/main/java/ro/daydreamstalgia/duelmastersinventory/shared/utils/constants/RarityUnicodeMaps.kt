package ro.daydreamstalgia.duelmastersinventory.shared.utils.constants


fun mapRarityToUnicode(rarity:String) = when(rarity) {
    "Common" -> "●"
    "Uncommon" -> "♦\uFE0E" // force text glyph
    "Rare" -> "★"
    "Very Rare" -> "✪"
    "Super Rare" -> "✜"
    else -> ""
}