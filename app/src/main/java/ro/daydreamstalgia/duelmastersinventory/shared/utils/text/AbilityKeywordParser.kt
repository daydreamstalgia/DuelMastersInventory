package ro.daydreamstalgia.duelmastersinventory.shared.utils.text

import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.AbilityKeyword

/**
 * Matches a prototype's ability text against the [AbilityKeyword] registry - case-insensitive
 * substring, e.g. "Blocker" inside "Blocker (This creature can block...)". Called once when a
 * CardPrototype is first created (CardSetImportRepository); not re-run if the registry grows
 * later, see specs/0007.
 */
fun matchAbilityKeywords(text: String?, registry: List<AbilityKeyword>): List<AbilityKeyword> {
    if (text.isNullOrBlank()) return emptyList()
    return registry.filter { text.contains(it.matchText, ignoreCase = true) }
}
