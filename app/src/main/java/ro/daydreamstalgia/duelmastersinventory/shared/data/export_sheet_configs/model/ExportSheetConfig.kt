package ro.daydreamstalgia.duelmastersinventory.shared.data.export_sheet_configs.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ExportSheetConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    val email: String, // google email for easy lookup

    val spreadsheetId: String, // as per cloud spreadsheets

    val title: String,

    val description: String,

    val recordType: String, // not really reflected but stuff like "card", "transaction" etc

    val columns: String, // comma-separated e.g. "name,rarity" or "price" etc

    val groups: String, // a sheet is created for each group
)