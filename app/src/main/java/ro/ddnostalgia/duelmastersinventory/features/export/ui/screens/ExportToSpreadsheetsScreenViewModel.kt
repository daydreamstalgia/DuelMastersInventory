package ro.ddnostalgia.duelmastersinventory.features.export.ui.screens

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.api.services.sheets.v4.Sheets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.ddnostalgia.duelmastersinventory.features.export.model.SheetTable
import ro.ddnostalgia.duelmastersinventory.features.export.service.SpreadsheetConfigDataProvider
import ro.ddnostalgia.duelmastersinventory.shared.data.GenericDao
import ro.ddnostalgia.duelmastersinventory.shared.data.export_sheet_configs.model.ExportSheetConfig
import ro.ddnostalgia.duelmastersinventory.shared.data.export_sheet_configs.repository.ExportSheetConfigRepository
import ro.ddnostalgia.duelmastersinventory.shared.service.SpreadsheetIdController
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.BaseViewModel
import javax.inject.Inject


@HiltViewModel
class ExportToSpreadsheetsScreenViewModel @Inject constructor(
    exportSheetConfigRepository: ExportSheetConfigRepository,
    dao: GenericDao
): BaseViewModel() {
    private val spreadsheetConfigDataProvider = SpreadsheetConfigDataProvider(dao)

    private var _email = MutableStateFlow<String?>(null)

    val email = _email.stateInViewModelScope()

    val spreadsheetConfigs = email.map {
        it?.let {
            exportSheetConfigRepository
                .getByEmail(it)
                .first()
        } ?: listOf()
    }.stateInViewModelScope()


    fun setEmail(value:String) {
        _email.update { value }
    }

    suspend fun exportToSpreadsheets(
        sheetsService: Sheets,
        config: ExportSheetConfig
    ) {
        withContext(Dispatchers.IO) {
            val sheets = getSheetTables(config)
            val controller = SpreadsheetIdController(sheetsService, config.spreadsheetId)
            controller.clear()

            var first = true

            for (sheet in sheets) {
                if(first) {
                    controller.renameFirstSheet(sheet.title)
                    first = false
                } else {
                    controller.createSheet(sheet.title)
                }
                controller.writeSheetTable(sheet)
            }

        }
    }

    private fun getSheetTables(
        config: ExportSheetConfig,
    ):Sequence<SheetTable>  {
        return spreadsheetConfigDataProvider.generateSheets(config)
    }
}