package ro.ddnostalgia.duelmastersinventory.shared.data.actors.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ActorAlias",
    foreignKeys = [
        ForeignKey(
            entity = Actor::class,
            parentColumns = ["id"],
            childColumns = ["actorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["actorId"])]
)
data class ActorAlias(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val actorId: Int,
    val platform: String,
    val username: String,
    val url: String? = null,
)
