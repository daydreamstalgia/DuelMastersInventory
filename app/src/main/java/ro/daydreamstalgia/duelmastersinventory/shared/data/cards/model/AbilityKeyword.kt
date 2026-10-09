package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A rules keyword (Blocker, Shield Trigger, ...) an ability's icon can be rendered for,
 * matched against [CardPrototype.text] at import time. Data-only, not a hardcoded enum,
 * so a future keyword can be added as a row (plus a UI-side icon mapping) without a
 * schema/parser change - see specs/0007-prototype-identity-normalization-ability-keywords.md.
 */
@Entity(tableName = "AbilityKeyword")
data class AbilityKeyword(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    /** Case-insensitive substring matched against CardPrototype.text, e.g. "Blocker". */
    val matchText: String,
    /** UI-side lookup key for the rendered icon (see AbilityKeywordIcon.kt) - an unrecognized key just renders no icon. */
    val iconKey: String,
)
