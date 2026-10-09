package ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.card_print

import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.sqlite.db.SimpleSQLiteQuery
import ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.generic.DataProvider
import ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.generic.getAllFromQuery
import ro.daydreamstalgia.duelmastersinventory.shared.data.GenericDao
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.dao.CardPrototypeDao
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrint
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrintUserData
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototype
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeAndPrintWithCount

class CardPrintDataProvider(
    dao: GenericDao
) : DataProvider<CardPrototypeAndPrintWithCount>(
    CardPrintParserConfig(),
    { getAll(dao) }
)

private fun getAll(dao:GenericDao) : Sequence<CardPrototypeAndPrintWithCount> {
    val query = SimpleSQLiteQuery(CardPrototypeDao.SELECT_ALL_PRINTS_AND_OWNED_COUNT)
    return getAllFromQuery(dao, query) {cursor ->

        val printId = cursor.getInt(cursor.getColumnIndexOrThrow("print_id"))

        val userDataCursor = dao.queryCursor(
            SimpleSQLiteQuery(
                "SELECT * FROM CardPrintUserData WHERE cardPrintId = ?",
                arrayOf(printId)
            )
        )
        val userData = userDataCursor.use { udCursor ->
            if (udCursor.moveToFirst()) {
                CardPrintUserData(
                    cardPrintId = udCursor.getInt(udCursor.getColumnIndexOrThrow("cardPrintId")),
                    wishlist = udCursor.getInt(udCursor.getColumnIndexOrThrow("wishlist")) != 0
                )
            } else null
        }

        CardPrototypeAndPrintWithCount(
            prototype = CardPrototype(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("prototype_id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("prototype_name")),
                civilization = cursor.getString(cursor.getColumnIndexOrThrow("prototype_civilization")),
                races = cursor.getString(cursor.getColumnIndexOrThrow("prototype_races")),
                mana = cursor.getIntOrNull(cursor.getColumnIndexOrThrow("prototype_mana")),
                power = cursor.getString(cursor.getColumnIndexOrThrow("prototype_power")),
                type = cursor.getString(cursor.getColumnIndexOrThrow("prototype_type")),
                text = cursor.getString(cursor.getColumnIndexOrThrow("prototype_text"))
            ),
            print = CardPrint(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("print_id")),
                cardPrototypeId = cursor.getInt(cursor.getColumnIndexOrThrow("print_cardPrototypeId")),
                language = cursor.getString(cursor.getColumnIndexOrThrow("print_language")),
                set = cursor.getString(cursor.getColumnIndexOrThrow("print_set")),
                setNumber = cursor.getString(cursor.getColumnIndexOrThrow("print_setNumber")),
                setCount = cursor.getString(cursor.getColumnIndexOrThrow("print_setCount")),
                rarity = cursor.getString(cursor.getColumnIndexOrThrow("print_rarity")),
                translatedName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("print_translatedName")),
                translatedText = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("print_translatedText"))
            ),
            count = cursor.getInt(cursor.getColumnIndexOrThrow("count")),
            userData = userData
        )
    }
}