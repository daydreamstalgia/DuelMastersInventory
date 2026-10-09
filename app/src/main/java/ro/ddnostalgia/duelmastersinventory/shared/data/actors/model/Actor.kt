package ro.ddnostalgia.duelmastersinventory.shared.data.actors.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Actor")
data class Actor(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firstName: String? = null,
    val lastName: String? = null,
)
