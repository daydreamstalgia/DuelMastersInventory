package ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.generic

import android.database.Cursor
import androidx.room.RoomSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import ro.daydreamstalgia.duelmastersinventory.shared.data.GenericDao

fun <T> getAllFromQuery(
    dao: GenericDao,
    query:SupportSQLiteQuery,
    mapper: (Cursor) -> T
): Sequence<T> = sequence {
    val cursor = dao.queryCursor(query)
    cursor.use {  // automatically closes cursor when done
        while (cursor.moveToNext()) {
            yield(mapper(cursor))
        }
    }
}
