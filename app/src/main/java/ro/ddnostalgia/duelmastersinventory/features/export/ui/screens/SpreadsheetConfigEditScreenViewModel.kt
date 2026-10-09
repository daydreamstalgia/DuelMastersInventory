package ro.ddnostalgia.duelmastersinventory.features.export.ui.screens

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.api.services.sheets.v4.Sheets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.ddnostalgia.duelmastersinventory.features.transactions.ui.screens.TransactionEditScreenViewModel
import ro.ddnostalgia.duelmastersinventory.shared.data.export_sheet_configs.model.ExportSheetConfig
import ro.ddnostalgia.duelmastersinventory.shared.data.export_sheet_configs.repository.ExportSheetConfigRepository
import ro.ddnostalgia.duelmastersinventory.shared.service.SpreadsheetController
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.BaseRouteViewModel
import javax.inject.Inject

@HiltViewModel
class SpreadsheetConfigEditScreenViewModel @Inject constructor(
    private val spreadsheetConfigRepository: ExportSheetConfigRepository,
    savedStateHandle: SavedStateHandle
): BaseRouteViewModel(savedStateHandle) {
    private val spreadsheetConfigId = routeArgIntOrNull("id")
    private val email = routeArgString("email")

    private val _spreadsheetConfig = MutableStateFlow(TransactionEditScreenViewModel.EditableTransaction())

    private val _form = MutableStateFlow(EditableSpreadsheetConfig())

    val form = _form.stateInViewModelScope(EditableSpreadsheetConfig())

    // The screen only lets the user edit title/description. recordType/columns/groups
    // use their own mini-DSL (groups in particular embeds commas inside each group's
    // `sets(...)` list) so it's kept as the untouched entity read from the db rather
    // than round-tripped through EditableSpreadsheetConfig's List<String> fields, to
    // avoid corrupting it on save.
    private var loadedConfig: ExportSheetConfig? = null

    fun loadSpreadsheetConfig() {
        if(spreadsheetConfigId==null) {
            _form.update {
                EditableSpreadsheetConfig(
                    id = null,
                    email = email
                )
            }
        } else {
            viewModelScope.launch {
                val config = spreadsheetConfigRepository.getById(spreadsheetConfigId).first() ?: return@launch
                loadedConfig = config
                _form.update {
                    EditableSpreadsheetConfig(
                        id = config.id,
                        email = config.email,
                        spreadsheetId = config.spreadsheetId,
                        title = config.title,
                        description = config.description,
                        recordType = config.recordType,
                        // Raw, verbatim strings from the stored entity, for read-only display
                        // only — this screen doesn't yet offer UI to change columns/groups, so
                        // they're kept separate from the (currently unused) editable `columns`/
                        // `groups` List<String> fields below rather than parsed/reformatted.
                        columnsDisplay = config.columns,
                        groupsDisplay = config.groups,
                    )
                }
            }
        }
    }

    fun updateForm(
        title: String? = null,
        description: String? = null,
        recordType: String? = null,
        columns: List<String>? = null,
        groups: List<String>? = null,
    ) {
        _form.update {
            it.copy(
                title = title ?: it.title,
                description = description ?: it.description,
                recordType = recordType ?: it.recordType,
                columns = columns ?: it.columns,
                groups = groups ?: it.groups
            )
        }

    }


    suspend fun saveChanges(sheetsService: Sheets) {
        Log.d("VM", "saveChanges")
        val form = _form.value

        if(form.id==null) {
            withContext(Dispatchers.IO) {
                val controller = SpreadsheetController(sheetsService)

                val spreadsheetId = controller.createEmptySpreadsheet(form.title ?: "")

                spreadsheetConfigRepository.insert(
                    ExportSheetConfig(
                        id = 0,
                        email = form.email ?: "",
                        spreadsheetId = spreadsheetId,
                        title = form.title ?: "",
                        description = form.description ?: "",
                        recordType = form.recordType ?: "",
                        columns = form.columns.joinToString(";"),
                        groups = form.groups.joinToString(";"),
                    )
                )
            }
        } else {
            val existing = loadedConfig ?: return
            withContext(Dispatchers.IO) {
                spreadsheetConfigRepository.update(
                    existing.copy(
                        title = form.title ?: existing.title,
                        description = form.description ?: existing.description,
                        // Explicit no-op round-trip: this screen has no UI to change these
                        // yet, so they're carried through verbatim from the loaded entity
                        // rather than left to `copy()`'s implicit "unspecified param keeps
                        // its receiver value" behavior — same result, but makes it obvious to
                        // the next editor that dropping this would NOT silently keep the data,
                        // it would need `form.columns`/`form.groups` wired up first.
                        recordType = existing.recordType,
                        columns = existing.columns,
                        groups = existing.groups,
                    )
                )
            }
        }

    }

    data class EditableSpreadsheetConfig(
        val id: Int? = null,
        val email: String? = null,
        val spreadsheetId: String? = null,
        val title: String? = null,
        val description: String? = null,
        val recordType: String? = null,
        val columns: List<String> = listOf(),
        val groups: List<String> = listOf(),
        val columnsDisplay: String = "",
        val groupsDisplay: String = "",
    )
}