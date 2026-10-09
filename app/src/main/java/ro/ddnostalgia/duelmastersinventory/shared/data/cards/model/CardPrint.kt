package ro.ddnostalgia.duelmastersinventory.shared.data.cards.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "CardPrint",
    foreignKeys = [
        ForeignKey(
            entity = CardPrototype::class,
            parentColumns = ["id"],
            childColumns = ["cardPrototypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CardPrint(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val cardPrototypeId: Int,

    val language: String,

    val set: String,

    val setNumber: String,

    val setCount: String,

    val rarity: String?,

    val translatedName: String? = null,

    val translatedText: String? = null
) {
    fun displayId() = "$language $set $setNumber/$setCount"
}
