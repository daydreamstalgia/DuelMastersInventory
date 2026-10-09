package ro.ddnostalgia.duelmastersinventory.shared.data.cards.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = CardPrint::class,
            parentColumns = ["id"],
            childColumns = ["cardPrintId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CardPrintUserData(
    @PrimaryKey
    val cardPrintId: Int,

    val wishlist: Boolean,
)