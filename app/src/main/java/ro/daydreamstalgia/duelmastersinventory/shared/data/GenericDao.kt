package ro.daydreamstalgia.duelmastersinventory.shared.data

import android.database.Cursor
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface GenericDao {
    @RawQuery
    fun queryCursor(query: SupportSQLiteQuery): Cursor
}