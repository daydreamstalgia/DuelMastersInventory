package ro.ddnostalgia.duelmastersinventory.shared.utils.text

private val WHITESPACE_RUN = Regex("\\s+")
private val DASH_OR_COLON_SEPARATOR = Regex("\\s*[\u2012\u2013\u2014\u2015:]\\s*")
private val MULTI_VALUE_SPLIT = Regex("\\s*[/,]\\s*")

/**
 * Collapses cosmetic authoring differences - whitespace runs, dash/colon clause
 * separators, curly quotes, blank-vs-null "no value" - before decisions/0011's exact-tuple
 * prototype match, so independently-authored packs describing the same card (e.g. a
 * hand-typed JP pack vs. the bundled EN pack) still collapse to one CardPrototype instead
 * of a duplicate purely over formatting. A blank string and a null both mean "no value"
 * (e.g. a Spell's races/power) but packs disagree on which one they encode for that -
 * collapsed to null on both sides so that disagreement alone doesn't block a match.
 * Match-time only - never applied to what's actually stored. See
 * specs/0007-prototype-identity-normalization-ability-keywords.md.
 */
fun normalizeForIdentityMatch(value: String?): String? {
    if (value == null) return null
    val normalized = value
        .replace('\u2018', '\'').replace('\u2019', '\'')
        .replace('\u201C', '"').replace('\u201D', '"')
        .replace(DASH_OR_COLON_SEPARATOR, " - ")
        .trim()
        .replace(WHITESPACE_RUN, " ")
    return normalized.ifEmpty { null }
}

/** [normalizeForIdentityMatch] plus canonicalizing a "/"- or ","-joined multi-value field's separator (civilization, races). */
fun normalizeMultiValueForIdentityMatch(value: String?): String? {
    val normalized = normalizeForIdentityMatch(value) ?: return null
    val parts = normalized.split(MULTI_VALUE_SPLIT).map { it.trim() }.filter { it.isNotEmpty() }
    return if (parts.isEmpty()) normalized else parts.joinToString(" / ")
}
