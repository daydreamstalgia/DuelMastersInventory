package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrint
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrintUserData
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrintWithPrototype
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardSetSummaryRow

@Dao
interface CardPrintDao {
    @Query("SELECT * FROM CardPrint")
    fun getAll(): Flow<List<CardPrint>>

    @Query("SELECT * FROM CardPrint WHERE id = :id LIMIT 1")
    fun getById(id: Int): Flow<CardPrint?>

    @Query("SELECT * FROM CardPrint WHERE cardPrototypeId = :prototypeId")
    fun getByCardPrototypeId(prototypeId: Int): Flow<List<CardPrint>>

    @Query("SELECT DISTINCT [set] FROM CardPrint ORDER BY [set] ASC")
    fun getAllSets(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(print: CardPrint)

    /** Like [insert], but returns the inserted row's id - used by set-pack import to name the print's stored image files. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertReturningId(print: CardPrint): Long

    // The dedup key set-pack import skips on: a print already catalogued
    // under this exact (language, set, setNumber, setCount) is assumed to be
    // the same physical card, regardless of which pack it came from.
    @Query(
        "SELECT COUNT(*) FROM CardPrint WHERE language = :language AND [set] = :set AND setNumber = :setNumber AND setCount = :setCount"
    )
    fun countByLanguageSetNumberCount(language: String, set: String, setNumber: String, setCount: String): Int

    // Reimport conflict detection: the existing row for this print identity, if any - the
    // fields to compare an incoming pack card against instead of just skipping it.
    @Query(
        "SELECT * FROM CardPrint WHERE language = :language AND [set] = :set AND setNumber = :setNumber AND setCount = :setCount LIMIT 1"
    )
    fun findByLanguageSetNumberCount(language: String, set: String, setNumber: String, setCount: String): CardPrint?

    @Update
    fun update(print: CardPrint)

    @Query("SELECT COUNT(*) FROM CardPrint")
    fun count(): Int

    // Sets screen: one summary row per (language, set) - total printings, currently-owned
    // copies, and how many TransactedCard rows reference a print in that set at all (the
    // delete-guard check, see decisions/0012 - there's no DB-level FK to lean on here).
    @Query(
        """
        SELECT cp.language AS language, cp.[set] AS setCode,
            COUNT(DISTINCT cp.id) AS totalPrints,
            COUNT(DISTINCT CASE WHEN tc.outTransactionId IS NULL THEN tc.cardPrintId END) AS ownedCount,
            COUNT(tc.id) AS transactedCount
        FROM CardPrint cp
        LEFT JOIN TransactedCard tc ON tc.cardPrintId = cp.id
        GROUP BY cp.language, cp.[set]
        """
    )
    fun getSetSummaries(): Flow<List<CardSetSummaryRow>>

    @Query("SELECT id FROM CardPrint WHERE language = :language AND [set] = :set")
    fun getIdsByLanguageSet(language: String, set: String): List<Int>

    @Query("DELETE FROM CardPrint WHERE language = :language AND [set] = :set")
    fun deleteByLanguageSet(language: String, set: String)

    @Query("SELECT * FROM CardPrint")
    fun getAllWithPrototype() : Flow<List<CardPrintWithPrototype>>

    @Query("SELECT * FROM CardPrintUserData WHERE cardPrintId=:cardPrintId")
    fun getUserDataById(cardPrintId: Int): Flow<CardPrintUserData?>

    @Query("SELECT * FROM CardPrintUserData WHERE cardPrintId IN (:cardPrintIds)")
    fun getUserDataByIds(cardPrintIds: List<Int>): Flow<List<CardPrintUserData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserData(userData: CardPrintUserData)



}