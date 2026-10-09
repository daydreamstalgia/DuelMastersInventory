package ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model

import androidx.room.Embedded
import androidx.room.Relation

data class ActorWithAliases(
    @Embedded
    val actor: Actor,

    @Relation(parentColumn = "id", entityColumn = "actorId")
    val aliases: List<ActorAlias>
)
