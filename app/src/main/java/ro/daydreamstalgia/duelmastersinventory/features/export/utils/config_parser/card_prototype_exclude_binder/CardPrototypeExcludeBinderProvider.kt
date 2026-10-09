package ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.card_prototype_exclude_binder

import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.sqlite.db.SimpleSQLiteQuery
import ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.card_print.CardPrintParserConfig
import ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.generic.DataProvider
import ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.generic.getAllFromQuery
import ro.daydreamstalgia.duelmastersinventory.shared.data.GenericDao
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.dao.CardPrototypeDao
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrint
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototype
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeAndPrintWithCount

class CardPrototypeExcludeBinderProvider(
    dao: GenericDao
) : DataProvider<CardPrototypeAndPrintWithCount>(
    CardPrototypeExcludeBinderConfig(),
    { getAll(dao) }
)

private fun getAll(dao: GenericDao): Sequence<CardPrototypeAndPrintWithCount> {
    val query = SimpleSQLiteQuery("""
        SELECT 
            sub.prototype_id,
            sub.prototype_name,
            sub.civilization AS prototype_civilization,
            sub.races AS prototype_races,
            sub.mana AS prototype_mana,
            sub.power AS prototype_power,
            sub.type AS prototype_type,
            sub.rarity AS print_rarity,
            sub.print_id,
            4 - SUM(sub.count) AS playset_count
        FROM (
            SELECT 
                cp.id AS prototype_id,
                cp.name AS prototype_name,
                cp.civilization AS civilization,
                cp.races AS races,
                cp.mana AS mana,
                cp.power AS power,
                cp.type AS type,
                cp.text AS text,
                cpr.rarity AS rarity,
                cpr.id AS print_id,
                CASE 
                    WHEN COUNT(TC.id) > 0 THEN COUNT(TC.id)-1 
                    ELSE 0 
                END AS count
            FROM CardPrototype cp
            INNER JOIN CardPrint cpr ON cp.id = cpr.cardPrototypeId
            LEFT JOIN TransactedCard TC ON cpr.id = TC.cardPrintId
            WHERE TC.outTransactionId IS NULL
            GROUP BY cpr.id
        ) AS sub
        GROUP BY sub.prototype_name
        HAVING playset_count > 0
        ORDER BY playset_count ASC, sub.print_id ASC;
    """)

    return getAllFromQuery(dao, query) { cursor ->
        CardPrototypeAndPrintWithCount(
            prototype = CardPrototype(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("prototype_id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("prototype_name")),
                civilization = cursor.getString(cursor.getColumnIndexOrThrow("prototype_civilization")) ?: "",
                races = cursor.getString(cursor.getColumnIndexOrThrow("prototype_races")) ?: "",
                mana = cursor.getInt(cursor.getColumnIndexOrThrow("prototype_mana")),
                power = cursor.getString(cursor.getColumnIndexOrThrow("prototype_power")) ?: "",
                type = cursor.getString(cursor.getColumnIndexOrThrow("prototype_type")) ?: "",
                text = ""
            ),
            print = CardPrint(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("print_id")),
                cardPrototypeId = cursor.getInt(cursor.getColumnIndexOrThrow("prototype_id")),
                language = "",              // default
                set = "",                   // default
                setNumber = "",             // default
                setCount = "",              // default
                rarity = cursor.getString(cursor.getColumnIndexOrThrow("print_rarity")) ?: "",
                translatedName = null,      // default
                translatedText = null       // default
            ),
            count = cursor.getInt(cursor.getColumnIndexOrThrow("playset_count")),
        )

    }
}

