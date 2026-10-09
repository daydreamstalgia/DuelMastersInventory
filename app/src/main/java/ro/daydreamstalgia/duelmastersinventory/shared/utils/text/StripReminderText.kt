package ro.daydreamstalgia.duelmastersinventory.shared.utils.text

/**
 * Removes parenthetical reminder text (e.g. "Shield trigger (When this spell
 * is put into your hand from your shield zone, you may cast it for no
 * cost.)") from card rules text before it's embedded for semantic search.
 *
 * This boilerplate is near-identical across a huge fraction of the card pool
 * (every shield-trigger card repeats the same explanation), so leaving it in
 * made those cards spuriously cluster together for any query touching its
 * words ("shield", "hand") regardless of what the card's actual unique
 * effect is. Keyword names (e.g. "Shield trigger" itself) are kept - only
 * the parenthesized explanation is dropped.
 */
fun stripReminderText(text: String): String {
    return text
        .replace(Regex("\\([^)]*\\)"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
