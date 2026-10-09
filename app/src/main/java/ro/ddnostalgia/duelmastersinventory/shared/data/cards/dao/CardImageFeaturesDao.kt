package ro.ddnostalgia.duelmastersinventory.shared.data.cards.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardImageFeatures

@Dao
interface CardImageFeaturesDao {
    @Query("SELECT * FROM CardImageFeatures")
    suspend fun getAll(): List<CardImageFeatures>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(features: List<CardImageFeatures>)

    @Query("SELECT COUNT(*) FROM CardImageFeatures")
    fun count(): Int

    @Query("DELETE FROM CardImageFeatures WHERE cardPrintId IN (:cardPrintIds)")
    suspend fun deleteByCardPrintIds(cardPrintIds: List<Int>)
}
