package ro.ddnostalgia.duelmastersinventory.shared.data.transactions.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.OwnedCopyProvenance
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.TransactedCard
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.TransactedCardWithPrint
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.TransactedCardWithPrintAndCount
import ro.ddnostalgia.duelmastersinventory.shared.utils.converters.DateConverter

@Dao
interface TransactedCardDao {

    @Query("""
        SELECT * FROM TransactedCard 
        WHERE 
            inTransactionId = :transactionId
        OR
            outTransactionId = :transactionId
    """)
    fun getCardsByTransactionId(
        transactionId: Int
    ): Flow<List<TransactedCardWithPrint>>

    @Query("""
        SELECT 
            MIN(tc.id) AS id,            -- pick a stable id (needed for embedding)
            tc.cardPrintId AS cardPrintId,
            tc.inTransactionId AS inTransactionId,
            tc.outTransactionId AS outTransactionId,
            tc.condition AS condition,
            COUNT(*) AS count
        FROM TransactedCard tc
        WHERE tc.inTransactionId = :transactionId
           OR tc.outTransactionId = :transactionId
        GROUP BY tc.cardPrintId, tc.inTransactionId, tc.outTransactionId
        ORDER BY (tc.outTransactionId IS NOT NULL) -- make not owned cards appear last
    """)
    fun getCardsByTransactionIdWithCount(
        transactionId: Int
    ): Flow<List<TransactedCardWithPrintAndCount>>


    @Query("""
        SELECT 
            tc.id,
            tc.cardPrintId AS cardPrintId,
            tc.inTransactionId AS inTransactionId,
            tc.outTransactionId AS outTransactionId,
            tc.condition
        FROM TransactedCard tc
        WHERE tc.outTransactionId IS NULL
        ORDER BY tc.cardPrintId ASC, condition DESC
    """)
    fun getOwnedTransactedCards(): Flow<List<TransactedCardWithPrint>>

    @TypeConverters(DateConverter::class)
    @Query("""
        SELECT
            tc.id AS transactedCardId,
            tc.cardPrintId AS cardPrintId,
            tc.condition AS condition,
            t.id AS transactionId,
            t.date AS date,
            t.channel AS channel,
            t.actorId AS actorId
        FROM TransactedCard tc
        JOIN CardPrint cp ON cp.id = tc.cardPrintId
        JOIN `Transaction` t ON t.id = tc.inTransactionId
        WHERE cp.cardPrototypeId = :prototypeId
          AND tc.outTransactionId IS NULL
        ORDER BY t.date DESC
    """)
    fun getOwnedCopiesWithTransactionByPrototypeId(prototypeId: Int): Flow<List<OwnedCopyProvenance>>

    @Query("""
        SELECT COUNT(*) FROM TransactedCard
        WHERE inTransactionId IN (SELECT id FROM `Transaction` WHERE actorId = :actorId)
    """)
    fun cardsInCountByActorId(actorId: Int): Flow<Int>

    // Drawer stat chip ("CARDS"): total owned physical copies.
    @Query("SELECT COUNT(*) FROM TransactedCard WHERE outTransactionId IS NULL")
    fun getOwnedCardsCount(): Flow<Int>

    // Sets screen delete guard (decisions/0012): TransactedCard.cardPrintId has no FK, so this
    // has to be checked in app code before a set's CardPrint rows are deleted.
    @Query("SELECT COUNT(*) FROM TransactedCard WHERE cardPrintId IN (:cardPrintIds)")
    suspend fun countByCardPrintIds(cardPrintIds: List<Int>): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<TransactedCard>)

    @Delete
    suspend fun deleteAll(items: List<TransactedCard>)

    @Query("""
        UPDATE TransactedCard
        SET outTransactionId = :outTransactionId
        WHERE id = :id
    """)
    suspend fun updateOutTransactionId(id: Int, outTransactionId: Int?)

    @Transaction
    suspend fun updateOutTransactionIds(items: List<TransactedCard>) {
        for (item in items) {
            updateOutTransactionId(item.id, item.outTransactionId)
        }
    }
}