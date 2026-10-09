package ro.ddnostalgia.duelmastersinventory.shared.data.cards.model

import androidx.room.Embedded

data class CardPrototypeAndPrint(
    @Embedded(prefix="prototype_") val prototype: CardPrototype,
    @Embedded(prefix="print_") val print: CardPrint,
)