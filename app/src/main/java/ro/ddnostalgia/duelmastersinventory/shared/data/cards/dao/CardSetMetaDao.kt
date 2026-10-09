package ro.ddnostalgia.duelmastersinventory.shared.data.cards.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardSetMeta

@Dao
interface CardSetMetaDao {
    @Query("SELECT * FROM CardSetMeta")
    fun getAll(): Flow<List<CardSetMeta>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meta: CardSetMeta)

    @Query("DELETE FROM CardSetMeta WHERE language = :language AND [set] = :set")
    suspend fun deleteByLanguageSet(language: String, set: String)
}
