package ro.daydreamstalgia.duelmastersinventory.shared.data.search.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototype
import ro.daydreamstalgia.duelmastersinventory.shared.data.search.model.CardPrototypeEmbedding

@Dao
interface CardPrototypeEmbeddingDao {
    @Query("SELECT * FROM CardPrototypeEmbedding")
    fun getAll(): List<CardPrototypeEmbedding>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(embedding: CardPrototypeEmbedding)

    @Query("SELECT COUNT(*) FROM CardPrototypeEmbedding")
    fun count(): Int

    /** Prototypes with no embedding row yet - incrementally backfilled after a set-pack import instead of the full-catalog scan DatabaseModule's onOpen does. */
    @Query("SELECT cp.* FROM CardPrototype cp WHERE cp.id NOT IN (SELECT cardPrototypeId FROM CardPrototypeEmbedding)")
    fun getPrototypesMissingEmbedding(): List<CardPrototype>
}
