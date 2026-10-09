package ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.card_print

import android.util.Log
import androidx.compose.ui.graphics.Color
import ro.daydreamstalgia.duelmastersinventory.features.export.model.SheetCell
import ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.generic.ParserConfig
import ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.generic.categoricalPaletteColor
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrint
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeAndPrintWithCount
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.mapRarityToUnicode

class CardPrintParserConfig : ParserConfig<CardPrototypeAndPrintWithCount>(
    { name, value -> resolveFilter(name, value) },
    { pattern -> resolveGroupBelonging(pattern) },
    { item, column -> getCell(item, column) },
    { item -> getColor(item) }
) {
}

private fun resolveFilter(
    name:String,
    value:String
): ((CardPrototypeAndPrintWithCount)->Boolean) {
    when(name) {
        "language" -> {
            val languages = ParserConfig.parseSimpleList(value)
            return { languages.contains(it.print.language) }
        }
        "count" -> {
            val (operation, args) = ParserConfig.parseFunction(value) ?: return { true }

            return when(operation) {
                "eq" -> { { it.count==args[0].toInt() } }
                "gt" -> { { it.count>args[0].toInt() } }
                "lt" -> { { it.count<args[0].toInt() } }
                "ge" -> { { it.count>=args[0].toInt() } }
                "le" -> { { it.count<=args[0].toInt() } }
                "between" -> { { args[0].toInt()<=it.count && it.count<=args[1].toInt() } }
                else -> { { true } }
            }
        }
    }

    return { true }
}

private fun resolveGroupBelonging(
    pattern:String
): (CardPrototypeAndPrintWithCount)->Boolean {
    return parseSetsGroup(pattern)
        ?: { true }
}


private fun parseSetsGroup(pattern: String): ((CardPrototypeAndPrintWithCount) -> Boolean)? {
    // Match "sets(X1,X2,...,XN)"
    val regex = Regex("""sets\(([^)]+)\)""")
    val match = regex.matchEntire(pattern) ?: return null

    // Extract the comma-separated values inside parentheses
    val setsList = match.groupValues[1].split(",").map { it.trim() }

    // Return lambda that checks if item's print.set is in the list
    return { item: CardPrototypeAndPrintWithCount ->
        setsList.contains(item.print.set)
    }
}



private fun getCell(
    item:CardPrototypeAndPrintWithCount,
    column:String
): SheetCell {
    return SheetCell(text = when(column) {
        "displayId" -> item.print.displayId()
        "name" -> item.prototype.name ?: ""
        "civilization" -> item.prototype.civilization ?: ""
        "type" -> item.prototype.type ?: ""
        "rarity" -> mapRarityToUnicode(item.print.rarity ?: "")
        "wishlist" -> item.userData?.wishlist?.let { "✔\uFE0F" } ?: ""
        else -> ""
    })
}

private fun getColor(
    item: CardPrototypeAndPrintWithCount
) = categoricalPaletteColor(palette, item.print.set.let {
    if(it=="Promo")
        "$it ${item.print.setNumber.firstOrNull()}/${item.print.setCount.firstOrNull()}"
    else
        it
})

private val palette = mutableMapOf<String, Color>()
