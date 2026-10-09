package ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model

import androidx.room.Embedded
import androidx.room.Relation
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrint

data class TransactedCardWithPrintAndCount(
    @Embedded
    val card: TransactedCard,

    val count: Int,

    @Relation(
        parentColumn = "cardPrintId",
        entityColumn = "id"
    )
    val print: CardPrint,
)