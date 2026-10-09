package ro.ddnostalgia.duelmastersinventory.shared.data.cards.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrototype
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeAndPrint
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeAndPrintWithCount
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeWithPrints
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.PrototypePrintsFilterParams


@Dao
interface CardPrototypeDao {
    @Query("SELECT * FROM CardPrototype")
    fun getAll(): Flow<List<CardPrototype>>

    /** One-shot (non-Flow) snapshot - set-pack import matches against this in-memory rather than re-querying per card, see specs/0007. */
    @Query("SELECT * FROM CardPrototype")
    fun getAllOnce(): List<CardPrototype>

    @Query("SELECT * FROM CardPrototype WHERE id = :id LIMIT 1")
    fun getById(id: Int): Flow<CardPrototype?>

    /** One-shot (non-Flow) lookup - reimport conflict detection needs a snapshot, not a subscription. */
    @Query("SELECT * FROM CardPrototype WHERE id = :id LIMIT 1")
    fun getByIdOnce(id: Int): CardPrototype?

    @Query("SELECT * FROM CardPrototype")
    fun getPrototypesWithPrints(): Flow<List<CardPrototypeWithPrints>>

    @Query("SELECT * FROM CardPrototype WHERE id = :id LIMIT 1")
    fun getPrototypeWithPrints(id: Int): Flow<CardPrototypeWithPrints?>

    @Query("""
        SELECT 
            cp.id AS prototype_id,
            cp.name AS prototype_name,
            cp.civilization AS prototype_civilization,
            cp.races AS prototype_races,
            cp.mana AS prototype_mana,
            cp.power AS prototype_power,
            cp.type AS prototype_type,
            cp.text AS prototype_text,
    
            cpr.id AS print_id,
            cpr.cardPrototypeId AS print_cardPrototypeId,
            cpr.language AS print_language,
            cpr.[set] AS print_set,
            cpr.setNumber AS print_setNumber,
            cpr.setCount AS print_setCount,
            cpr.rarity AS print_rarity,
            cpr.translatedName AS print_translatedName,
            cpr.translatedText AS print_translatedText
        FROM CardPrototype cp
        INNER JOIN CardPrint cpr ON cp.id = cpr.cardPrototypeId
        ORDER BY cpr.id
    """)
    fun getPrototypePrintsJoined(): Flow<List<CardPrototypeAndPrint>>

    @Query(FILTER_QUERY)
    fun filterPrototypePrintsJoinedStrict(
        search: String?,
        rarities: List<String>,
        sets: List<String>,
        types: List<String>,
        languages: List<String>,
        civ0: String?,
        civ1: String?,
        civ2: String?,
        civ3: String?,
        civ4: String?,
        raritiesSize: Int = rarities.size,
        setsSize: Int = sets.size,
        typesSize: Int = types.size,
        languagesSize: Int = languages.size,
        civsSize: Int = listOf(civ0, civ1, civ2, civ3, civ4)
            .filter { civ->civ!=null }
            .size
    ) : Flow<List<CardPrototypeAndPrint>>

    @Query(FILTER_QUERY_WITH_OWNED_COUNT)
    fun filterPrototypePrintsJoinedStrictWithOwnedCount(
        search: String?,
        rarities: List<String>,
        sets: List<String>,
        types: List<String>,
        languages: List<String>,
        civ0: String?,
        civ1: String?,
        civ2: String?,
        civ3: String?,
        civ4: String?,
        raritiesSize: Int = rarities.size,
        setsSize: Int = sets.size,
        typesSize: Int = types.size,
        languagesSize: Int = languages.size,
        civsSize: Int = listOf(civ0, civ1, civ2, civ3, civ4)
            .filter { civ->civ!=null }
            .size
    ) : Flow<List<CardPrototypeAndPrintWithCount>>


    @Query(SELECT_PRINTS_AND_OWNED_COUNT_BY_PROTOTYPE_ID)
    fun getPrintsAndOwnedCountByPrototypeId(
        prototypeId: Int
    ): Flow<List<CardPrototypeAndPrintWithCount>>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(prototype: CardPrototype)

    /** Like [insert], but returns the inserted row's id - used by set-pack import to link a freshly-created prototype to its print. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertReturningId(prototype: CardPrototype): Long

    @Query("SELECT COUNT(*) FROM CardPrototype")
    fun count(): Int

    // Drawer stat chip ("UNIQUE"): count of distinct prototypes with at least one owned copy.
    @Query("""
        SELECT COUNT(DISTINCT cpr.cardPrototypeId)
        FROM TransactedCard tc
        INNER JOIN CardPrint cpr ON cpr.id = tc.cardPrintId
        WHERE tc.outTransactionId IS NULL
    """)
    fun getOwnedUniquePrototypeCount(): Flow<Int>

    companion object {
        const val SELECT_PRINTS_AND_OWNED_COUNT_BY_PROTOTYPE_ID = """
            SELECT P.*, COUNT(TC.id) as count FROM ($SELECT_CARD_PROTOTYPE_AND_PRINT) AS P 
            LEFT JOIN TransactedCard TC ON P.print_id = TC.cardPrintId
            WHERE prototype_id = :prototypeId AND TC.outTransactionId IS NULL
            GROUP BY P.print_id
        """

        const val SELECT_ALL_PRINTS_AND_OWNED_COUNT = """
            SELECT P.*, COUNT(TC.id) as count FROM ($SELECT_CARD_PROTOTYPE_AND_PRINT) AS P 
            LEFT JOIN TransactedCard TC ON P.print_id = TC.cardPrintId
            WHERE TC.outTransactionId IS NULL
            GROUP BY P.print_id
        """

        const val SELECT_ALL_PROTOTYPES_AND_OWNED_COUNT = """
            SELECT P.*, COUNT(TC.id) as count FROM ($SELECT_CARD_PROTOTYPE_AND_PRINT) AS P 
            LEFT JOIN TransactedCard TC ON P.print_id = TC.cardPrintId
            WHERE TC.outTransactionId IS NULL
            GROUP BY P.prototype_id
        """
    }
}

private const val SELECT_CARD_PROTOTYPE_AND_PRINT = """
    SELECT 
        cp.id AS prototype_id,
        cp.name AS prototype_name,
        cp.civilization AS prototype_civilization,
        cp.races AS prototype_races,
        cp.mana AS prototype_mana,
        cp.power AS prototype_power,
        cp.type AS prototype_type,
        cp.text AS prototype_text,

        cpr.id AS print_id,
        cpr.cardPrototypeId AS print_cardPrototypeId,
        cpr.language AS print_language,
        cpr.[set] AS print_set,
        cpr.setNumber AS print_setNumber,
        cpr.setCount AS print_setCount,
        cpr.rarity AS print_rarity,
        cpr.translatedName AS print_translatedName,
        cpr.translatedText AS print_translatedText
    FROM CardPrototype cp
    INNER JOIN CardPrint cpr ON cp.id = cpr.cardPrototypeId
"""

private const val FILTER_QUERY = """
    SELECT * FROM (
            SELECT 
                cp.id AS prototype_id,
                cp.name AS prototype_name,
                cp.civilization AS prototype_civilization,
                cp.races AS prototype_races,
                cp.mana AS prototype_mana,
                cp.power AS prototype_power,
                cp.type AS prototype_type,
                cp.text AS prototype_text,
        
                cpr.id AS print_id,
                cpr.cardPrototypeId AS print_cardPrototypeId,
                cpr.language AS print_language,
                cpr.[set] AS print_set,
                cpr.setNumber AS print_setNumber,
                cpr.setCount AS print_setCount,
                cpr.rarity AS print_rarity,
                cpr.translatedName AS print_translatedName,
                cpr.translatedText AS print_translatedText,
                
                
                ( CAST((:civ0 IS NOT NULL AND cp.civilization LIKE '%' || :civ0 || '%') AS INT)
                + CAST((:civ1 IS NOT NULL AND cp.civilization LIKE '%' || :civ1 || '%') AS INT)
                + CAST((:civ2 IS NOT NULL AND cp.civilization LIKE '%' || :civ2 || '%') AS INT)
                + CAST((:civ3 IS NOT NULL AND cp.civilization LIKE '%' || :civ3 || '%') AS INT)
                + CAST((:civ4 IS NOT NULL AND cp.civilization LIKE '%' || :civ4 || '%') AS INT)
                ) AS civilization_matches_count
            FROM CardPrototype cp
            INNER JOIN CardPrint cpr ON cp.id = cpr.cardPrototypeId
            WHERE 
                (:search IS NULL OR LOWER(cp.name) LIKE '%' || LOWER(:search) || '%' 
                 OR LOWER(cp.races) LIKE '%' || LOWER(:search) || '%'
                 OR LOWER(cp.text) LIKE '%' || LOWER(:search) || '%'
                 OR LOWER(cpr.translatedName) LIKE '%' || LOWER(:search) || '%'
                 OR LOWER(cpr.translatedText) LIKE '%' || LOWER(:search) || '%')
            AND (:raritiesSize = 0 OR cpr.rarity IN (:rarities))
            AND (:setsSize = 0 OR cpr.[set] IN (:sets))
            AND (:typesSize = 0 OR cp.type IN (:types))
            AND (:languagesSize = 0 OR cpr.language IN (:languages))
            ORDER BY cpr.id
        )
        WHERE (:civsSize = 0 OR :civsSize = civilization_matches_count)    
"""

private const val FILTER_QUERY_WITH_OWNED_COUNT = """
    SELECT P.*, COUNT(TC.id) as count FROM ($FILTER_QUERY) AS P
        LEFT JOIN TransactedCard TC ON P.print_id = TC.cardPrintId
        WHERE TC.outTransactionId IS NULL
        GROUP BY P.print_id
"""