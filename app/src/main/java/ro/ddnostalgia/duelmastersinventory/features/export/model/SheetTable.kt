package ro.ddnostalgia.duelmastersinventory.features.export.model

import androidx.compose.ui.graphics.Color

class SheetTable(
    val title: String,
    val header: List<String>,
    val rows: Sequence<SheetRow>
)

class SheetRow(
    val background: Color? = null,
    val cells: List<SheetCell>
)

class SheetCell(
    val text: String,
)