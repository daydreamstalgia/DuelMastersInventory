package ro.ddnostalgia.duelmastersinventory.shared.data.cards.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "CardPrototype")
data class CardPrototype(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String?,
    val civilization: String?,
    val races: String?,
    val mana: Int?,
    val power: String?,
    val type: String?,
    val text: String?,
)