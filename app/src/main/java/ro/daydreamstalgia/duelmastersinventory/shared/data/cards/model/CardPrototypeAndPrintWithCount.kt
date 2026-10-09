package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model

import androidx.room.Embedded
import androidx.room.Relation

data class CardPrototypeAndPrintWithCount(
    @Embedded(prefix="prototype_") val prototype: CardPrototype,
    @Embedded(prefix="print_") val print: CardPrint,
    val count: Int = 0,

    @Relation(
        parentColumn = "print_id",          // primary key of CardPrint
        entityColumn = "cardPrintId"       // foreign key in CardPrintUserData
    )
    val userData: CardPrintUserData? = null
)