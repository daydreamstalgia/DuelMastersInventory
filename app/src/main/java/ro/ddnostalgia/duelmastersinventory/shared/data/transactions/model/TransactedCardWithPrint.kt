package ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model

import androidx.room.Embedded
import androidx.room.Relation
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrint
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrintWithPrototype

data class TransactedCardWithPrint(
    @Embedded
    val card: TransactedCard,

    @Relation(
        entity = CardPrint::class,
        parentColumn = "cardPrintId",
        entityColumn = "id"
    )
    private val printWithPrototype: CardPrintWithPrototype
) {
    val print get() = printWithPrototype.print
    val cardPrototype get() = printWithPrototype.prototype

    fun copyWithoutId(): TransactedCardWithPrint = copy(
        card = card.copy(id=0),
        printWithPrototype = printWithPrototype
    )

}
