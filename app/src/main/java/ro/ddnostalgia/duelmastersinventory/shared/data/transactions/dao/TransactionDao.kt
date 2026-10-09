package ro.ddnostalgia.duelmastersinventory.shared.data.transactions.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.Transaction

/**
 * Per-actor deal count + net EUR, grouped in one query so the Actors list
 * doesn't issue one aggregate query per row. Same sign convention as
 * [TransactionDao.netCostByActorId]/StatisticsDao.getTotalCost (INBOUND +, OUTBOUND -).
 */
data class ActorDealsSummary(
    val actorId: Int,
    val dealsCount: Int,
    val netCost: Double,
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM `Transaction` ORDER BY date DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM `Transaction` WHERE id=:id")
    fun getById(id:Int): Flow<Transaction?>

    @Query("""
        SELECT * FROM `Transaction`
        WHERE
            (:search IS NULL
             OR LOWER(description) LIKE '%' || LOWER(:search) || '%'
             OR LOWER(channel) LIKE '%' || LOWER(:search) || '%'
             OR actorId IN (
                SELECT id FROM Actor
                WHERE LOWER(firstName) LIKE '%' || LOWER(:search) || '%'
                   OR LOWER(lastName) LIKE '%' || LOWER(:search) || '%'
                UNION
                SELECT actorId FROM ActorAlias
                WHERE LOWER(username) LIKE '%' || LOWER(:search) || '%'
             ))
        ORDER BY date DESC
    """)
    fun filter(
        search: String?
    ) : Flow<List<Transaction>>

    @Query("SELECT DISTINCT channel FROM `Transaction` WHERE channel IS NOT NULL ORDER BY channel")
    fun getDistinctChannels(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM `Transaction` WHERE actorId=:actorId")
    fun countByActorId(actorId: Int): Flow<Int>

    @Query("""
        SELECT COALESCE(SUM(costEuro * CASE WHEN type='INBOUND' THEN 1 ELSE -1 END), 0)
        FROM `Transaction` WHERE actorId=:actorId
    """)
    fun netCostByActorId(actorId: Int): Flow<Double>

    @Query("""
        SELECT
            actorId AS actorId,
            COUNT(*) AS dealsCount,
            COALESCE(SUM(costEuro * CASE WHEN type='INBOUND' THEN 1 ELSE -1 END), 0) AS netCost
        FROM `Transaction`
        GROUP BY actorId
    """)
    fun getDealsSummaryByActor(): Flow<List<ActorDealsSummary>>

    @Query("SELECT * FROM `Transaction` WHERE actorId=:actorId ORDER BY date DESC LIMIT :limit")
    fun getRecentByActorId(actorId: Int, limit: Int): Flow<List<Transaction>>

    @Query("UPDATE `Transaction` SET actorId=:targetActorId WHERE actorId=:sourceActorId")
    suspend fun reassignActor(sourceActorId: Int, targetActorId: Int)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: Transaction) : Long

    @Update
    suspend fun update(transaction: Transaction) : Int
}