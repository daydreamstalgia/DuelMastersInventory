package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model

import androidx.room.Embedded
import androidx.room.Relation

data class CardPrototypeWithPrints(
    @Embedded val prototype: CardPrototype,
    @Relation(
        parentColumn = "id",
        entityColumn = "cardPrototypeId"
    )
    val prints: List<CardPrint>
)