package ro.ddnostalgia.duelmastersinventory.features.export.ui.screens

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import ro.ddnostalgia.duelmastersinventory.nav.Routes
import ro.ddnostalgia.duelmastersinventory.shared.data.export_sheet_configs.repository.ExportSheetConfigRepository
import ro.ddnostalgia.duelmastersinventory.shared.utils.types.BaseRouteViewModel
import javax.inject.Inject

@HiltViewModel
class SpreadsheetConfigViewScreenViewModel @Inject constructor(
    spreadsheetConfigRepository: ExportSheetConfigRepository,
    savedStateHandle: SavedStateHandle,
) : BaseRouteViewModel(savedStateHandle) {
    private val spreadsheetConfigId = routeArgInt(Routes.SpreadsheetConfigView.spreadsheetConfigIdArg)

    val spreadsheetConfig = spreadsheetConfigRepository
        .getById(spreadsheetConfigId)
        .stateInViewModelScope()

}