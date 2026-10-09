package ro.ddnostalgia.duelmastersinventory.shared.service

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.AddSheetRequest
import com.google.api.services.sheets.v4.model.AutoResizeDimensionsRequest
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.CellData
import com.google.api.services.sheets.v4.model.CellFormat
import com.google.api.services.sheets.v4.model.DeleteSheetRequest
import com.google.api.services.sheets.v4.model.DimensionProperties
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.GridRange
import com.google.api.services.sheets.v4.model.RepeatCellRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.TextFormat
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ro.ddnostalgia.duelmastersinventory.features.export.model.SheetTable

class SpreadsheetController(
    private val sheetsService: Sheets,
) {

    /**
     * Creates an empty spreadsheet with the given title.
     * Returns the Spreadsheet ID.
     */
    suspend fun createEmptySpreadsheet(title: String): String {
        val spreadsheet = Spreadsheet()
            .setProperties(
                SpreadsheetProperties().setTitle(title)
            )

        val created = sheetsService.spreadsheets()
            .create(spreadsheet)
            .execute()

        return created.spreadsheetId
            ?: throw IllegalStateException("Spreadsheet creation returned null ID")
    }
}

class SpreadsheetIdController(
    private val sheetsService: Sheets,
    private val spreadsheetId: String
) {

    /**
     * Clears all sheets except leaves a blank spreadsheet (one sheet required by API).
     */
    suspend fun clear() {
        withContext(Dispatchers.IO) {
            val spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
            val sheets = spreadsheet.sheets
            val requests = mutableListOf<Request>()

            // Delete all sheets except the first one
            sheets.drop(1).forEach { sheet ->
                requests.add(
                    Request().setDeleteSheet(
                        DeleteSheetRequest().setSheetId(sheet.properties.sheetId)
                    )
                )
            }

            // Clear all content & formatting in the first sheet
            val firstSheetId = sheets.first().properties.sheetId
            requests.add(
                Request().setRepeatCell(
                    RepeatCellRequest()
                        .setRange(GridRange().setSheetId(firstSheetId))
                        .setCell(CellData()) // empty cell resets values & formatting
                        .setFields("*")
                )
            )

            if (requests.isNotEmpty()) {
                val batchUpdate = BatchUpdateSpreadsheetRequest().setRequests(requests)
                sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdate).execute()
            }
        }
    }


    /**
     * Creates a new sheet with the given name.
     */
    suspend fun createSheet(name:String) {
        withContext(Dispatchers.IO) {
            val addSheetRequest = AddSheetRequest()
                .setProperties(
                    SheetProperties().setTitle(name)
                )

            val request = Request().setAddSheet(addSheetRequest)
            val batchUpdate = BatchUpdateSpreadsheetRequest().setRequests(listOf(request))
            sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdate).execute()
        }
    }

    suspend fun renameFirstSheet(newName: String) = withContext(Dispatchers.IO) {
        // Get the spreadsheet and the first sheet
        val spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
        val firstSheet = spreadsheet.sheets.firstOrNull() ?: return@withContext

        val request = Request().setUpdateSheetProperties(
            UpdateSheetPropertiesRequest()
                .setProperties(
                    SheetProperties()
                        .setSheetId(firstSheet.properties.sheetId)
                        .setTitle(newName)
                )
                .setFields("title") // Only update the title
        )

        val batchUpdate = BatchUpdateSpreadsheetRequest().setRequests(listOf(request))
        sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdate).execute()
    }


    suspend fun deleteSheet(name: String) = withContext(Dispatchers.IO) {
        // Get the spreadsheet and its sheets
        val spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
        val sheets = spreadsheet.sheets

        // Find the sheet with the given name
        val targetSheet = sheets.firstOrNull { it.properties.title == name }
            ?: return@withContext // Nothing to delete

        // Avoid deleting the last remaining sheet
        if (sheets.size <= 1) return@withContext

        val request = Request().setDeleteSheet(
            DeleteSheetRequest().setSheetId(targetSheet.properties.sheetId)
        )

        val batchUpdate = BatchUpdateSpreadsheetRequest().setRequests(listOf(request))
        sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdate).execute()
    }


    /**
     * Writes a SheetTable to a sheet. The sheet must exist.
     * First row = headers (bold), then rows from table.rows.
     */
    suspend fun writeSheetTable(table: SheetTable) {
        withContext(Dispatchers.IO) {
            // 1) Ensure the sheet exists and retrieve its sheetId.
            //    If it doesn't exist, create it and obtain the new sheet id.
            var spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
            // try find existing
            var sheetProps = spreadsheet.sheets
                .firstOrNull { it.properties.title == table.title }
                ?.properties

            if (sheetProps == null) {
                // create sheet
                val addReq = Request().setAddSheet(AddSheetRequest().setProperties(SheetProperties().setTitle(table.title)))
                val batch = BatchUpdateSpreadsheetRequest().setRequests(listOf(addReq))
                val batchResp = sheetsService.spreadsheets().batchUpdate(spreadsheetId, batch).execute()
                // extract created sheet properties
                sheetProps = batchResp.replies
                    ?.firstOrNull()
                    ?.addSheet
                    ?.properties
                    ?: throw IllegalStateException("Failed to create sheet ${table.title}")
                // refresh spreadsheet object
                spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
            }

            val sheetId = sheetProps.sheetId

            // Utility: escape sheet title for A1 notation (single quotes doubled).
            fun escapeSheetTitleForRange(title: String): String =
                "'${title.replace("'", "''")}'"

            // 2) Build values and background requests in a single iteration over the sequence.
            val values = mutableListOf<List<Any>>()
            // pad headers with spaces to give a bit of visual padding on auto-resize
            values.add(table.header.map { " $it " })

            val backgroundRequests = mutableListOf<Request>()

            // we need an index for the data rows; header occupies row 0, so first data row is index 1
            var dataRowIndex = 0
            for (row in table.rows) {
                // add row values (pad each cell)
                values.add(row.cells.map { " ${it.text} " })

                // if row has background color, create a RepeatCellRequest for that row range
                row.background?.let { color ->
                    // Convert Compose Color (red/green/blue/alpha floats 0..1) to Sheets Color
                    val sheetsColor = com.google.api.services.sheets.v4.model.Color().apply {
                        this.red = color.red
                        this.green = color.green
                        this.blue = color.blue
                        // If you prefer, use color.alpha; Sheets color alpha may be unsupported in some contexts,
                        // but set it anyway.
                        this.alpha = 1f
                    }

                    val gridRange = GridRange().apply {
                        this.sheetId = sheetId
                        this.startRowIndex = dataRowIndex + 1       // +1 because header is row 0
                        this.endRowIndex = dataRowIndex + 2         // exclusive
                        this.startColumnIndex = 0
                        this.endColumnIndex = table.header.size
                    }

                    val repeatReq = RepeatCellRequest().apply {
                        this.range = gridRange
                        this.cell = CellData().apply {
                            this.userEnteredFormat = CellFormat().apply {
                                this.backgroundColor = sheetsColor
                            }
                        }
                        this.fields = "userEnteredFormat.backgroundColor"
                    }

                    backgroundRequests.add(Request().setRepeatCell(repeatReq))
                }

                dataRowIndex++
            }

            // 3) Write values (header + rows) in one Values.update call.
            val range = "${escapeSheetTitleForRange(table.title)}!A1"
            val body = ValueRange().setValues(values)
            sheetsService.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute()

            // 4) Prepare formatting + auto-resize requests.
            // Bold header row:
            val headerRange = GridRange().apply {
                this.sheetId = sheetId
                this.startRowIndex = 0
                this.endRowIndex = 1
                this.startColumnIndex = 0
                this.endColumnIndex = table.header.size
            }

            val boldHeaderReq = Request().setRepeatCell(
                RepeatCellRequest().apply {
                    this.range = headerRange
                    this.cell = CellData().apply {
                        this.userEnteredFormat = CellFormat().apply {
                            this.textFormat = TextFormat().setBold(true)
                        }
                    }
                    this.fields = "userEnteredFormat.textFormat.bold"
                }
            )

            // Auto-resize columns (COLUMNS dimension, exclusive end index)
            val autoResizeReq = Request().setAutoResizeDimensions(
                AutoResizeDimensionsRequest().apply {
                    this.dimensions = DimensionRange().apply {
                        this.sheetId = sheetId
                        this.dimension = "COLUMNS"
                        this.startIndex = 0
                        this.endIndex = table.header.size
                    }
                }
            )

            // 5) Send batch update: header bold, auto-resize, then all background requests
            val allRequests = mutableListOf<Request>()
            allRequests.add(boldHeaderReq)
            allRequests.add(autoResizeReq)
            allRequests.addAll(backgroundRequests)

            if (allRequests.isNotEmpty()) {
                val batchUpdate = BatchUpdateSpreadsheetRequest().setRequests(allRequests)
                sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdate).execute()
            }
        }
    }
}
