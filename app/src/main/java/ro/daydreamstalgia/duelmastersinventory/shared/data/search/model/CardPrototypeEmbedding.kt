package ro.daydreamstalgia.duelmastersinventory.shared.data.search.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototype
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Precomputed [SemanticSearchEngine]-produced sentence embedding for a
 * `CardPrototype`'s composite searchable text (name + races + text), seeded
 * once on first run so semantic search only has to embed the query at
 * search time.
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = CardPrototype::class,
            parentColumns = ["id"],
            childColumns = ["cardPrototypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CardPrototypeEmbedding(
    @PrimaryKey
    val cardPrototypeId: Int,

    val vector: ByteArray,
) {
    fun toFloatArray(): FloatArray {
        val buffer = ByteBuffer.wrap(vector).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(vector.size / Float.SIZE_BYTES) { buffer.float }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CardPrototypeEmbedding) return false
        return cardPrototypeId == other.cardPrototypeId && vector.contentEquals(other.vector)
    }

    override fun hashCode(): Int {
        return 31 * cardPrototypeId + vector.contentHashCode()
    }

    companion object {
        fun fromFloatArray(cardPrototypeId: Int, vector: FloatArray): CardPrototypeEmbedding {
            val buffer = ByteBuffer.allocate(vector.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
            for (v in vector) buffer.putFloat(v)
            return CardPrototypeEmbedding(cardPrototypeId, buffer.array())
        }
    }
}
