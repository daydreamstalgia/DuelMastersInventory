package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model

/**
 * Canonical release order for card sets, keyed by print (language, set) pair:
 * EN (DM-01..DM-12, CTD, Promo), then DE, then any other non-JP language
 * (alphabetical by language code), then JP last - each language bucket
 * internally ordered by [SET_SEQUENCE], the shared release-order list (a set
 * code means the same physical set - and thus the same release rank -
 * regardless of which language it's printed in). Sets not yet in
 * [SET_SEQUENCE] (a future pack not hand-added here yet) sort after all known
 * ones within their language bucket, alphabetically - append them to
 * [SET_SEQUENCE] in real release-date order as they're catalogued instead of
 * relying on that fallback long-term. This same ranking also drives the
 * one-time "reorder card print IDs" settings action
 * ([ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardPrintReorderRepository]),
 * which physically renumbers `CardPrint.id` to match - see
 * decisions/0019-card-print-id-canonical-reorder.md in the KB.
 */
object CardSetOrder {

    private val SET_SEQUENCE = listOf(
        "DM-01", "DM-02", "DM-03", "DM-04", "DM-05", "DM-06",
        "DM-07", "DM-08", "DM-09", "DM-10", "DM-11", "DM-12",
        "CTD", "Promo",
    )

    const val BUCKET_EN = 0
    const val BUCKET_DE = 1
    const val BUCKET_OTHER = 2
    const val BUCKET_JP = 3

    /** Language grouping bucket used by [comparator] and by the print-id reorder tool. */
    fun languageBucket(language: String): Int = when (language) {
        "EN" -> BUCKET_EN
        "DE" -> BUCKET_DE
        "JP" -> BUCKET_JP
        else -> BUCKET_OTHER
    }

    /** Release-order rank of [set] within its language, shared across all languages. */
    fun setRank(set: String): Int = SET_SEQUENCE.indexOf(set).let { if (it >= 0) it else Int.MAX_VALUE }

    private val DISPLAY_NAMES: Map<Pair<String, String>, String> = mapOf(
        ("EN" to "DM-01") to "Base Set",
        ("EN" to "DM-02") to "Evo-Crushinators of Doom",
        ("EN" to "DM-03") to "Rampage of the Super Warriors",
        ("EN" to "DM-04") to "Shadowclash of Blinding Night",
        ("EN" to "DM-05") to "Survivors of the Megapocalypse",
        ("EN" to "DM-06") to "Stomp-A-Trons of Invincible Wrath",
        ("EN" to "DM-07") to "Thundercharge of Ultra Destruction",
        ("EN" to "DM-08") to "Epic Dragons of Hyperchaos",
        ("EN" to "DM-09") to "Fatal Brood of Infinite Ruin",
        ("EN" to "DM-10") to "Shockwaves of the Shattered Rainbow",
        ("EN" to "DM-11") to "Blast-o-Splosion of Gigantic Rage",
        ("EN" to "DM-12") to "Thrash of the Hybrid Megacreatures",
        ("EN" to "CTD") to "Collector's Tin Deck",
        ("EN" to "Promo") to "Promo",
        ("JP" to "DM-01") to "Basic Set",
    )

    /** Sorts `(language, set)` pairs into canonical release order; ties broken alphabetically. */
    val comparator: Comparator<Pair<String, String>> = Comparator { a, b ->
        val bucketCompare = languageBucket(a.first).compareTo(languageBucket(b.first))
        if (bucketCompare != 0) return@Comparator bucketCompare
        if (languageBucket(a.first) == BUCKET_OTHER) {
            val langCompare = a.first.compareTo(b.first)
            if (langCompare != 0) return@Comparator langCompare
        }
        val rankCompare = setRank(a.second).compareTo(setRank(b.second))
        if (rankCompare != 0) return@Comparator rankCompare
        "${a.first} ${a.second}".compareTo("${b.first} ${b.second}")
    }

    // Names for sets imported at runtime via a set pack (see "dynamic sets"
    // spec) - not known at compile time like [DISPLAY_NAMES], so they're
    // loaded from CardSetMeta once at DB open and refreshed after every
    // import. @Volatile since DatabaseModule's onOpen callback populates this
    // from a background thread while the UI reads it on the main thread.
    @Volatile
    private var dynamicNames: Map<Pair<String, String>, String> = emptyMap()

    fun loadDynamicNames(entries: List<Pair<Pair<String, String>, String>>) {
        dynamicNames = entries.toMap()
    }

    /** Real set name for a print's (language, set), or null if not in the table yet. */
    fun displayName(language: String, set: String): String? =
        DISPLAY_NAMES[language to set] ?: dynamicNames[language to set]
}
