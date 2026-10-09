package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model

import androidx.room.Embedded
import androidx.room.Relation

data class CardPrintWithPrototype(
    @Embedded
    val print: CardPrint,

    @Relation(
        parentColumn = "cardPrototypeId",
        entityColumn = "id"
    )
    val prototype: CardPrototype
)
