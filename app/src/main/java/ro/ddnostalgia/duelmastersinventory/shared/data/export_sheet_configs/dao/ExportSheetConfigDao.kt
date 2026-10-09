package ro.ddnostalgia.duelmastersinventory.shared.data.export_sheet_configs.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ro.ddnostalgia.duelmastersinventory.shared.data.export_sheet_configs.model.ExportSheetConfig


@Dao
interface ExportSheetConfigDao {
    @Query("SELECT * FROM ExportSheetConfig where id=:id")
    fun getById(id:Int): Flow<ExportSheetConfig?>

    @Query("SELECT * FROM ExportSheetConfig where email=:email")
    fun getByEmail(email:String): Flow<List<ExportSheetConfig>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ExportSheetConfig): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(item: ExportSheetConfig): Int
}