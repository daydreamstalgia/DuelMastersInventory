package ro.daydreamstalgia.duelmastersinventory.features.export.service

import android.util.Log
import androidx.room.util.foreignKeyCheck
import ro.daydreamstalgia.duelmastersinventory.features.export.model.SheetCell
import ro.daydreamstalgia.duelmastersinventory.features.export.model.SheetRow
import ro.daydreamstalgia.duelmastersinventory.features.export.model.SheetTable
import ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.card_print.CardPrintDataProvider
import ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.card_print.CardPrintParserConfig
import ro.daydreamstalgia.duelmastersinventory.features.export.utils.config_parser.card_prototype_exclude_binder.CardPrototypeExcludeBinderProvider
import ro.daydreamstalgia.duelmastersinventory.shared.data.GenericDao
import ro.daydreamstalgia.duelmastersinventory.shared.data.export_sheet_configs.model.ExportSheetConfig
import java.time.LocalDate
import java.time.LocalDateTime

class SpreadsheetConfigDataProvider(
    private val dao: GenericDao
) {
    fun generateSheets(
        sheetConfig: ExportSheetConfig
    ): Sequence<SheetTable> {
        val (recordName, filtersStr) = parseRecord(sheetConfig.recordType)
        val columns = sheetConfig.columns.split(",")
        val groupsStr = parseGroups(sheetConfig.groups)
        Log.d("SpreadsheetConfigDataProvider", "$recordName -> $filtersStr")
        Log.d("SpreadsheetConfigDataProvider", "$columns")
        Log.d("SpreadsheetConfigDataProvider", "$groupsStr")

        val sheets = when (recordName) {
            "cardPrint" -> CardPrintDataProvider(dao).generateSheets(
                columns, filtersStr, groupsStr
            )

            "cardPrototypeExcludeBinder" -> CardPrototypeExcludeBinderProvider(dao)
                .generateSheets(columns, filtersStr, groupsStr)

            else -> sequence { }
        }

        return sequence {
            for (sheet in sheets) {
                yield(sheet)
            }

            yield(getInfoSheetTable(sheetConfig))
        }
    }

    private fun parseGroups(input: String): List<Pair<String, String>> {
        val regex = "\"([^\"]+)\"=([^,]+\\([^)]*\\))".toRegex()
        return regex.findAll(input).map { match ->
            val name = match.groupValues[1]
            val pattern = match.groupValues[2].trim()
            name to pattern
        }.toList()
    }

    private fun parseRecord(input: String): Pair<String, Map<String, String>> {
        // Match "recordName(...)" pattern
        val regex = """(\w+)\((.*)\)""".toRegex()
        val match = regex.matchEntire(input) ?: throw IllegalArgumentException("Invalid format")

        val recordName = match.groupValues[1]
        val inside = match.groupValues[2]

        // Split key=value pairs. This naive split works if values do not contain unbalanced commas.
        val map = mutableMapOf<String, String>()
        val pairRegex = """(\w+)\s*=\s*(.*?)(?=(?:,\s*\w+=)|$)""".toRegex()
        pairRegex.findAll(inside).forEach { m ->
            val key = m.groupValues[1]
            val value = m.groupValues[2].trim()
            map[key] = value
        }

        return recordName to map
    }


    private fun getInfoSheetTable(sheetConfig: ExportSheetConfig): SheetTable {
        val (recordName, filtersStr) = parseRecord(sheetConfig.recordType)
        return SheetTable(
            title = "autogen_SheetInfo",
            header = listOf("Property", "Value"),
            rows = sequence {
                yield(buildSimpleSheetRow(listOf("Title", sheetConfig.title)))
                yield(buildSimpleSheetRow(listOf("Description", sheetConfig.description)))
                yield(buildSimpleSheetRow(listOf("Record", recordName)))
                yield(buildSimpleSheetRow(listOf("Filter", filtersStr.map { "${it.key}=${it.value}" }.joinToString(","))))
                yield(buildSimpleSheetRow(listOf("Last updated", LocalDateTime.now().toString())))
            }
        )
    }

    private fun buildSimpleSheetRow(values: List<String>) = SheetRow(
        cells = values.map { SheetCell(text=it) }
    )

}