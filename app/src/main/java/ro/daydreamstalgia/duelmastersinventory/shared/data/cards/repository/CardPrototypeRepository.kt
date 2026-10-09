package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ro.daydreamstalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeAndPrint
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeAndPrintWithCount
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeWithPrints
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeWithPrintsAndCount
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.FilterMode
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.PrototypePrintsFilterParams
import ro.daydreamstalgia.duelmastersinventory.shared.data.search.SemanticSearchEngine
import ro.daydreamstalgia.duelmastersinventory.shared.utils.text.fuzzySimilarity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardPrototypeRepository @Inject constructor(
    db: DuelMastersInventoryDatabase,
    private val semanticSearchEngine: SemanticSearchEngine,
) {
    private val dao = db.cardPrototypeDao()
    private val embeddingDao = db.cardPrototypeEmbeddingDao()
    private val abilityKeywordDao = db.abilityKeywordDao()

    // Loaded once and reused across searches instead of re-reading & re-parsing
    // ~1200 BLOB rows from Room on every keystroke. Embeddings are seeded once
    // at first app run and never change afterward, so caching is safe for the
    // app's lifetime - except a search can land mid-seed (first run / first
    // update), so an empty snapshot is deliberately not cached, letting the
    // next search retry rather than being stuck with zero semantic matches.
    @Volatile
    private var cachedEmbeddings: Map<Int, FloatArray>? = null

    private fun embeddingsSnapshot(): Map<Int, FloatArray> {
        cachedEmbeddings?.let { return it }
        val loaded = embeddingDao.getAll().associate { it.cardPrototypeId to it.toFloatArray() }
        if (loaded.isNotEmpty()) cachedEmbeddings = loaded
        return loaded
    }

    fun getCardPrototypesWithPrints(): Flow<List<CardPrototypeWithPrints>>
        = groupCardPrototypesAndPrints(dao.getPrototypePrintsJoined())

    fun getCardPrototypesWithPrintsById(prototypeId: Int) : Flow<CardPrototypeWithPrints?>
        = dao.getPrototypeWithPrints(prototypeId)

    fun getAbilityKeywordsForPrototype(prototypeId: Int) = abilityKeywordDao.getForPrototype(prototypeId)

    fun filterCardPrototypesWithPrints(
        params: PrototypePrintsFilterParams,
        mode: FilterMode
    ): Flow<List<CardPrototypeWithPrints>> {
        val flow = dao.filterPrototypePrintsJoinedStrict(
            params.search,
            params.rarities,
            params.sets,
            params.types,
            params.languages,
            params.civilizations.getOrNull(0),
            params.civilizations.getOrNull(1),
            params.civilizations.getOrNull(2),
            params.civilizations.getOrNull(3),
            params.civilizations.getOrNull(4)
        )

        return groupCardPrototypesAndPrints(flow)
    }

    fun getPrintsAndOwnedCountByPrototypeId(
        prototypeId: Int
    ): Flow<List<CardPrototypeAndPrintWithCount>> = dao.getPrintsAndOwnedCountByPrototypeId(
        prototypeId
    )

    fun getOwnedUniquePrototypeCount(): Flow<Int> = dao.getOwnedUniquePrototypeCount()

    fun filterCardPrototypeWithPrintsAndOwnedCount(
        params: PrototypePrintsFilterParams,
        mode: FilterMode
    ): Flow<List<CardPrototypeWithPrintsAndCount>> {
        // Search is intentionally NOT passed to the DAO (always null here) -
        // every other filter (rarity/set/type/language/civilization) still runs
        // in SQL, but search matching/ranking (substring + fuzzy + semantic) is
        // done below in Kotlin so it can blend those three signals.
        val flow = dao.filterPrototypePrintsJoinedStrictWithOwnedCount(
            null,
            params.rarities,
            params.sets,
            params.types,
            params.languages,
            params.civilizations.getOrNull(0),
            params.civilizations.getOrNull(1),
            params.civilizations.getOrNull(2),
            params.civilizations.getOrNull(3),
            params.civilizations.getOrNull(4)
        )

        val grouped = groupCardPrototypesAndPrintsWithCount(flow)

        val search = params.search
        if (search.isNullOrBlank()) return grouped

        return rankBySearch(grouped, search)
    }

    private fun rankBySearch(
        flow: Flow<List<CardPrototypeWithPrintsAndCount>>,
        search: String
    ): Flow<List<CardPrototypeWithPrintsAndCount>> {
        // embed() (TFLite inference) and embeddingDao.getAll() (a blocking Room
        // query) both do real work - keep them off the collector's dispatcher
        // (ViewModel code collects this on Dispatchers.Main) or they crash with
        // "Cannot access database on the main thread".
        return flow.map { list ->
            withContext(Dispatchers.IO) {
                val startedAt = System.currentTimeMillis()
                val queryLower = search.lowercase()
                val queryEmbedding = semanticSearchEngine.embed(search)
                val embeddedAt = System.currentTimeMillis()

                val embeddings = embeddingsSnapshot()

                val scored = list.map { card -> card to scoreCard(card, queryLower, queryEmbedding, embeddings) }

                // Semantic noise floor varies a lot by query (measured: ~0.1-0.25 for
                // one query, ~0.55-0.65 for another), so a single fixed cutoff can't
                // work for all of them - a query with a weak best match would show
                // nothing at a strict fixed threshold, while a query with a strong
                // best match would still let mediocre noise through at a loose one.
                // Anchor the cutoff to this query's own top semantic score instead.
                val topSemanticScore = scored.maxOf { it.second.semanticScore }
                val semanticCutoff = maxOf(SEMANTIC_MATCH_FLOOR, topSemanticScore - SEMANTIC_MATCH_MARGIN)

                val result = scored
                    .filter { (_, score) ->
                        score.substringHit ||
                            score.fuzzyScore >= FUZZY_MATCH_THRESHOLD ||
                            score.semanticScore >= semanticCutoff
                    }
                    .sortedWith(
                        compareByDescending<Pair<CardPrototypeWithPrintsAndCount, SearchScore>> { it.second.substringHit }
                            .thenByDescending { maxOf(it.second.fuzzyScore, it.second.semanticScore) }
                            .thenBy { it.first.prototype.id }
                    )
                    .map { (card, _) -> card }

                android.util.Log.d(
                    "CardPrototypeRepository",
                    "search '$search': embed=${embeddedAt - startedAt}ms rank=${System.currentTimeMillis() - embeddedAt}ms " +
                        "total=${System.currentTimeMillis() - startedAt}ms results=${result.size}/${list.size}"
                )

                result
            }
        }
    }

    private fun scoreCard(
        card: CardPrototypeWithPrintsAndCount,
        queryLower: String,
        queryEmbedding: FloatArray,
        embeddings: Map<Int, FloatArray>
    ): SearchScore {
        val prototype = card.prototype
        val substringHit = listOfNotNull(prototype.name, prototype.races, prototype.text)
            .any { it.lowercase().contains(queryLower) } ||
            card.prints.any {
                listOfNotNull(it.translatedName, it.translatedText).any { text ->
                    text.lowercase().contains(queryLower)
                }
            }

        val fuzzyScore = prototype.name?.let { fuzzyNameScore(queryLower, prototype.id, it) } ?: 0f
        val semanticScore = embeddings[prototype.id]
            ?.let { SemanticSearchEngine.cosineSimilarity(queryEmbedding, it) }
            ?: 0f

        return SearchScore(
            substringHit = substringHit,
            fuzzyScore = fuzzyScore,
            semanticScore = semanticScore
        )
    }

    // Card names are often "Character Name, Title" (e.g. "Natasha, Fire Whip");
    // comparing a typo'd query against the whole name unfairly penalizes it for
    // not also matching the title, so also score it against each individual word.
    private fun fuzzyNameScore(queryLower: String, prototypeId: Int, name: String): Float {
        return nameCandidatesFor(prototypeId, name).maxOf { fuzzySimilarity(queryLower, it) }
    }

    // `Regex(...)` compiles a pattern on every call - building it fresh per
    // card per search (thousands of times per keystroke) was the actual
    // bottleneck here, not the fuzzy-matching math itself. Cache both the
    // compiled pattern and each prototype's word-split result, since a name
    // never changes after the catalog is seeded.
    private val nameCandidatesCache = java.util.concurrent.ConcurrentHashMap<Int, List<String>>()

    private fun nameCandidatesFor(prototypeId: Int, name: String): List<String> {
        return nameCandidatesCache.getOrPut(prototypeId) {
            val lower = name.lowercase()
            val words = lower.split(NAME_SPLIT_REGEX).filter { it.isNotEmpty() }
            words + lower
        }
    }

    private data class SearchScore(val substringHit: Boolean, val fuzzyScore: Float, val semanticScore: Float)

    companion object {
        // Normalized Levenshtein similarity (1 - distance/maxLen) inflates scores
        // for SHORT words compared against a longer (or vice versa) query, purely
        // from length mismatch, independent of real similarity - e.g. "aqua" (4
        // chars) vs. an unrelated 6-char query lands at exactly 0.5 by the raw
        // math, not because they're actually alike. Measured: single-character
        // real typos (e.g. "Nastasha" vs "Natasha") score ~0.875. 0.75 sits well
        // above the observed short-word-mismatch noise (~0.5) and comfortably
        // below genuine typos, so fuzzy match is intentionally strict - it's only
        // meant to catch near-identical misspellings, not loose similarity.
        private const val FUZZY_MATCH_THRESHOLD = 0.75f

        // Absolute floor for a semantic-only match, AND semanticCutoff below
        // (see rankBySearch) adapts this per-query relative to that query's own
        // top score, since the noise floor varies hugely by query (measured
        // ~0.1-0.25 noise for one query, ~0.55-0.65 for another) - a single fixed
        // cutoff can't be both loose enough to return anything for a query with a
        // weak best match and strict enough to reject noise for a query with a
        // strong one.
        private const val SEMANTIC_MATCH_FLOOR = 0.45f
        private const val SEMANTIC_MATCH_MARGIN = 0.12f

        private val NAME_SPLIT_REGEX = Regex("[^\\p{L}\\p{N}]+")
    }


    // Grouping key includes setNumber (not just prototype/set/language) so that distinct
    // print variants sharing a set+language - e.g. a secret-rare and a super-rare printing
    // of the same card in the same set, differing only by collector number/rarity, such as
    // "Ballom, Master of Death" in DMX-21 (set_number "㊙3" vs "38") - each get their own
    // grid entry instead of silently collapsing into one tile that only ever showed the
    // first print. Genuinely identical duplicate CardPrint rows (same setNumber too) still
    // merge, which is the only case this grouping now exists to catch.
    private fun groupCardPrototypesAndPrintsWithCount(
        flow: Flow<List<CardPrototypeAndPrintWithCount>>
    ) : Flow<List<CardPrototypeWithPrintsAndCount>> {
        return flow.map { list ->
            list.groupBy {
                listOf(it.prototype.id, it.print.set, it.print.language, it.print.setNumber)
            }.values.map { grouped ->
                CardPrototypeWithPrintsAndCount(
                    prototype = grouped.first().prototype,
                    prints = grouped.map { it.print },
                    count = grouped.sumOf { it.count }
                )
            }
        }
    }

    private fun groupCardPrototypesAndPrints(
        flow: Flow<List<CardPrototypeAndPrint>>
    ) : Flow<List<CardPrototypeWithPrints>> {
        return flow.map { list ->
            list.groupBy {
                listOf(it.prototype.id, it.print.set, it.print.language, it.print.setNumber)
            }.values.map { grouped ->
                CardPrototypeWithPrints(
                    prototype = grouped.first().prototype,
                    prints = grouped.map { it.print },
                )
            }
        }
    }

}