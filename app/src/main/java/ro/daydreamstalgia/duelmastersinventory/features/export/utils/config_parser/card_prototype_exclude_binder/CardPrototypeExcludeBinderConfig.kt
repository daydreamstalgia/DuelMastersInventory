package ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.card_prototype_exclude_binder

import ro.daydreamstalgia.duelmastersinventory.features.export.model.SheetCell
import ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.generic.ParserConfig
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeAndPrintWithCount
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.mapRarityToUnicode

class CardPrototypeExcludeBinderConfig : ParserConfig<CardPrototypeAndPrintWithCount>(
    { name, value -> { true } },
    { pattern -> { true } },
    { item, column -> getCell(item, column) },
) {
}

private fun getCell(
    item: CardPrototypeAndPrintWithCount,
    column:String
): SheetCell {
    return SheetCell(text = when(column) {
        "displayId" -> item.print.displayId()
        "name" -> item.prototype.name ?: ""
        "civilization" -> item.prototype.civilization ?: ""
        "type" -> item.prototype.type ?: ""
        "rarity" -> mapRarityToUnicode(item.print.rarity ?: "")
        "count" -> "${item.count}"
        else -> ""
    })
}