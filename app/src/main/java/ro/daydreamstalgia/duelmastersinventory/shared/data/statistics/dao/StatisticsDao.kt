package ro.daydreamstalgia.duelmastersinventory.shared.data.statistics.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ro.daydreamstalgia.duelmastersinventory.shared.data.statistics.model.LabeledCost

@Dao
interface StatisticsDao {
    @Query("""
        SELECT 
            strftime('%Y-%m', date) AS label,
            $TRANSACTION_GROUP_COST_FIELDS_SUM
        FROM `Transaction`
        GROUP BY strftime('%Y-%m', date)
        ORDER BY label
    """)
    fun getCostByMonth() : Flow<List<LabeledCost>>

    @Query("""
        SELECT 
            'total' as label,
            $TRANSACTION_GROUP_COST_FIELDS_SUM
        FROM `Transaction`
    """)
    fun getTotalCost(): Flow<LabeledCost>

    @Query("""
        SELECT 
            'average' AS label,
            CASE 
                WHEN COUNT(*) = 0 THEN 0
                WHEN julianday(MAX(date)) = julianday(MIN(date))
                    THEN SUM(CASE WHEN type = 'INBOUND'  THEN costEuro ELSE 0 END)
                ELSE SUM(CASE WHEN type = 'INBOUND'  THEN costEuro ELSE 0 END) / ((julianday(MAX(date)) - julianday(MIN(date))) / 30.0)
            END AS inboundCost,
    
            CASE 
                WHEN COUNT(*) = 0 THEN 0
                WHEN julianday(MAX(date)) = julianday(MIN(date))
                    THEN SUM(CASE WHEN type = 'OUTBOUND' THEN costEuro ELSE 0 END)
                ELSE SUM(CASE WHEN type = 'OUTBOUND' THEN costEuro ELSE 0 END) / ((julianday(MAX(date)) - julianday(MIN(date))) / 30.0)
            END AS outboundCost,
    
            CASE 
                WHEN COUNT(*) = 0 THEN 0
                WHEN julianday(MAX(date)) = julianday(MIN(date))
                    THEN SUM(costEuro * CASE 
                            WHEN type = 'INBOUND' THEN +1
                            WHEN type = 'OUTBOUND' THEN -1
                            ELSE +1
                        END)
                ELSE SUM(costEuro * CASE 
                            WHEN type = 'INBOUND' THEN +1
                            WHEN type = 'OUTBOUND' THEN -1
                            ELSE +1
                        END) / ((julianday(MAX(date)) - julianday(MIN(date))) / 30.0)
            END AS cost
        FROM `Transaction`
    """)
    fun getMonthlyAverageCost(): Flow<LabeledCost>

    @Query("""
        WITH stats AS (
            SELECT
                -- signed total cost
                SUM(costEuro * CASE 
                    WHEN type = 'INBOUND' THEN +1
                    WHEN type = 'OUTBOUND' THEN -1
                    ELSE +1
                END) AS totalCost,

                -- months since first transaction until now
                CASE 
                    WHEN COUNT(*) = 0 THEN 0
                    ELSE (
                        (CAST(strftime('%Y', 'now') AS INTEGER) - 
                         CAST(strftime('%Y', MIN(date)) AS INTEGER)) * 12
                        +
                        (CAST(strftime('%m', 'now') AS INTEGER) - 
                         CAST(strftime('%m', MIN(date)) AS INTEGER))
                    )
                END AS monthsSinceFirst
            FROM `Transaction`
        )
        SELECT
            CASE 
                WHEN :targetAverage = 0 THEN 0
                ELSE CAST(
                    (totalCost / :targetAverage) - monthsSinceFirst
                    AS INTEGER
                )
            END
        FROM stats
    """)
    fun getNextBuyMonthSpan(targetAverage: Int): Flow<Int>
}

private const val TRANSACTION_GROUP_COST_FIELDS_SUM = """
    SUM(CASE WHEN type = 'INBOUND'  THEN costEuro ELSE 0 END) AS inboundCost,
    SUM(CASE WHEN type = 'OUTBOUND' THEN costEuro ELSE 0 END) AS outboundCost,
    SUM(costEuro * CASE 
        WHEN type = 'INBOUND' THEN +1
        WHEN type = 'OUTBOUND' THEN -1
        ELSE +1
    END) AS cost
"""