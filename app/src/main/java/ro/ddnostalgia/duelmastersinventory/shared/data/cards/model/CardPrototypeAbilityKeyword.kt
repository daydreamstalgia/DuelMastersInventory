package ro.ddnostalgia.duelmastersinventory.shared.data.cards.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Many-to-many: which [AbilityKeyword]s were found in a [CardPrototype]'s text, computed
 * once when the prototype is first created (see CardSetImportRepository.matchOrCreatePrototype).
 * Not recomputed if the keyword registry grows later - see specs/0007's "no backfill" scope note.
 */
@Entity(
    tableName = "CardPrototypeAbilityKeyword",
    primaryKeys = ["cardPrototypeId", "abilityKeywordId"],
    foreignKeys = [
        ForeignKey(
            entity = CardPrototype::class,
            parentColumns = ["id"],
            childColumns = ["cardPrototypeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AbilityKeyword::class,
            parentColumns = ["id"],
            childColumns = ["abilityKeywordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("abilityKeywordId")],
)
data class CardPrototypeAbilityKeyword(
    val cardPrototypeId: Int,
    val abilityKeywordId: Int,
)
