package ro.ddnostalgia.duelmastersinventory.shared.data.cards.model

import androidx.room.Entity

/**
 * Display name for a (language, set) pair that was learned at runtime from an
 * imported set pack's `data.json` ("set_name"), rather than known ahead of
 * time like the bundled sets in [CardSetOrder]'s static table. Populated by
 * `CardSetImportRepository` on every successful import (including re-imports
 * of an already-known set, so a corrected name overwrites the old one).
 */
@Entity(tableName = "CardSetMeta", primaryKeys = ["language", "set"])
data class CardSetMeta(
    val language: String,
    val set: String,
    val name: String,
)
