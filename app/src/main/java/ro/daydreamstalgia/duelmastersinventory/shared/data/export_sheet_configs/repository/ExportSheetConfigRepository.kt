package ro.daydreamstalgia.duelmastersinventory.shared.data.export_sheet_configs.repository

import kotlinx.coroutines.flow.Flow
import ro.daydreamstalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.daydreamstalgia.duelmastersinventory.shared.data.export_sheet_configs.dao.ExportSheetConfigDao
import ro.daydreamstalgia.duelmastersinventory.shared.data.export_sheet_configs.model.ExportSheetConfig
import java.lang.Exception
import java.lang.RuntimeException
import javax.inject.Inject

class ExportSheetConfigRepository @Inject constructor (
    db:DuelMastersInventoryDatabase
) {
    val dao = db.exportSheetConfigDao()

    fun getByEmail(email:String) = dao.getByEmail(email)

    fun getById(id:Int) = dao.getById(id)

    suspend fun insert(item: ExportSheetConfig): ExportSheetConfig {
        val id = dao.insert(item).toInt()
        return item.copy(id=id)
    }

    suspend fun update(item: ExportSheetConfig): ExportSheetConfig {
        val rows = dao.update(item)
        if(rows==1) {
            return item
        }
        throw RuntimeException("Failed to updated sheet config.")
    }

}