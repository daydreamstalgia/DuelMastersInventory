package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ro.daydreamstalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrint
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototype
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardSetMeta
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.AbilityKeyword
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototypeAbilityKeyword
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardSetOrder
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardImageMatchRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.search.SemanticSearchEngine
import ro.daydreamstalgia.duelmastersinventory.shared.data.search.model.CardPrototypeEmbedding
import ro.daydreamstalgia.duelmastersinventory.shared.utils.image.ImageSimilarity
import ro.daydreamstalgia.duelmastersinventory.shared.utils.text.matchAbilityKeywords
import ro.daydreamstalgia.duelmastersinventory.shared.utils.text.normalizeForIdentityMatch
import ro.daydreamstalgia.duelmastersinventory.shared.utils.text.normalizeMultiValueForIdentityMatch
import ro.daydreamstalgia.duelmastersinventory.shared.utils.text.stripReminderText
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class SetImportResult(
    val setLanguage: String,
    val setCode: String,
    val setName: String,
    val cardsAdded: Int,
    val cardsUpdated: Int,
    val cardsSkipped: Int,
    val prototypesCreated: Int,
)

/** Status line + optional 0..1 completion for the import dialog (see specs/0004). Null fraction for steps that can't be sized up front. */
data class ImportProgress(val message: String, val fraction: Float? = null)

enum class ConflictResolution { KEEP_OLD, TAKE_NEW }

data class FieldDiff(val label: String, val old: String?, val new: String?)

enum class AssetDiffKind { ADDED, CHANGED }

/** One already-catalogued card whose incoming pack data/art differs from what's stored - see specs/0004 and decisions/0012. */
data class SetImportConflict(
    val setNumber: String,
    val setCount: String,
    val existingPrintId: Int,
    val displayName: String,
    val fieldDiffs: List<FieldDiff>,
    val assetDiff: AssetDiffKind?,
)

/** Result of [CardSetImportRepository.scanZip] - nothing is written to the DB yet. */
data class SetImportPlan(
    val pack: SetPackData,
    val entries: Map<String, ByteArray>,
    val newCount: Int,
    val identicalCount: Int,
    val conflicts: List<SetImportConflict>,
)

/** [FieldDiff.label]s that come from CardPrototype rather than CardPrint - a change here means the exact-tuple match key changed, so decisions/0012's repoint rule applies instead of an in-place update. */
private val CORE_PROTOTYPE_FIELDS = setOf("Name", "Civilization", "Races", "Mana", "Power", "Type", "Text")

/** Thumbnail width for the internally-generated `card_images_low` copy, matching the bundled low-res assets' proportions (403x560 -> 100x140). */
private const val LOW_RES_WIDTH = 100

/**
 * Imports a set pack zip (see "dynamic sets" spec: `data.json` + PNG art under `assets/`,
 * downloaded via Discover or picked from the filesystem) into the local catalog, in two phases:
 *
 * 1. [scanZip] reads the pack and classifies every card as new, identical-to-existing (silently
 *    skipped, same as before specs/0004), or a conflict (existing print with different field
 *    values and/or art) - nothing is written yet.
 * 2. [applyPlan] does the actual writes, given the caller's chosen [ConflictResolution] per
 *    conflict (see specs/0004-sets-management-and-reimport-conflicts.md for the UI this drives).
 *
 * A brand-new card is matched to an existing [CardPrototype] by exact (name, civilization, races,
 * mana, power, type, text) after normalizing cosmetic formatting differences (whitespace, dash/
 * colon separators, quote style - specs/0007) - a reprint describes the same card identically,
 * e.g. DM-06's "Death Smoke" matches DM-01's prototype rather than creating a duplicate - or a
 * new prototype is created if nothing matches (decisions/0011). The new [CardPrint] gets a fresh autoGenerate id,
 * and its art is decoded from the zip and written to internal storage via [CardImageStore] rather
 * than APK assets, since assets can't be written to at runtime.
 */
@Singleton
class CardSetImportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: DuelMastersInventoryDatabase,
    private val cardImageMatchRepository: CardImageMatchRepository,
    private val semanticSearchEngine: SemanticSearchEngine,
) {
    private val gson = Gson()

    suspend fun scanZip(
        zipStream: InputStream,
        onProgress: (ImportProgress) -> Unit = {},
    ): SetImportPlan = withContext(Dispatchers.IO) {
        onProgress(ImportProgress("Reading set pack..."))
        val entries = readZipEntries(zipStream)
        val dataJsonBytes = entries["data.json"]
            ?: throw IllegalArgumentException("Not a valid set pack: missing data.json")
        val pack = gson.fromJson(String(dataJsonBytes, Charsets.UTF_8), SetPackData::class.java)

        val cardPrototypeDao = db.cardPrototypeDao()
        val cardPrintDao = db.cardPrintDao()

        var newCount = 0
        var identicalCount = 0
        val conflicts = mutableListOf<SetImportConflict>()

        val total = pack.cards.size.coerceAtLeast(1)
        pack.cards.forEachIndexed { index, card ->
            onProgress(
                ImportProgress(
                    "Scanning ${pack.set_language} ${pack.set_code}: card ${index + 1}/${pack.cards.size}",
                    (index + 1f) / total,
                )
            )

            val existingPrint = cardPrintDao.findByLanguageSetNumberCount(
                pack.set_language, pack.set_code, card.set_number, card.set_count
            )
            if (existingPrint == null) {
                newCount++
                return@forEachIndexed
            }

            val existingPrototype = cardPrototypeDao.getByIdOnce(existingPrint.cardPrototypeId)
            val fieldDiffs = buildFieldDiffs(existingPrototype, existingPrint, card)

            val incomingAssetBytes = assetBytesFor(entries, pack, card)
            val assetDiff = incomingAssetBytes?.let { detectAssetDiff(existingPrint.id, it) }

            if (fieldDiffs.isEmpty() && assetDiff == null) {
                identicalCount++
            } else {
                conflicts.add(
                    SetImportConflict(
                        setNumber = card.set_number,
                        setCount = card.set_count,
                        existingPrintId = existingPrint.id,
                        displayName = card.name ?: existingPrototype?.name ?: "${card.set_number}/${card.set_count}",
                        fieldDiffs = fieldDiffs,
                        assetDiff = assetDiff,
                    )
                )
            }
        }

        SetImportPlan(pack, entries, newCount, identicalCount, conflicts)
    }

    /** [resolutions] keyed by [SetImportConflict.existingPrintId]; a conflict with no entry defaults to [ConflictResolution.TAKE_NEW] (see specs/0004 - "new" is pre-selected in the UI). */
    suspend fun applyPlan(
        plan: SetImportPlan,
        resolutions: Map<Int, ConflictResolution>,
        onProgress: (ImportProgress) -> Unit = {},
    ): SetImportResult = withContext(Dispatchers.IO) {
        val pack = plan.pack
        val entries = plan.entries
        val cardPrintDao = db.cardPrintDao()
        val conflictsByPrintId = plan.conflicts.associateBy { it.existingPrintId }

        var cardsAdded = 0
        var cardsUpdated = 0
        var cardsSkipped = 0
        var prototypesCreated = 0
        val touchedPrintIds = mutableListOf<Int>()

        // Snapshot once per import rather than re-querying per card (specs/0007) - a card
        // matched/created earlier in this same pack must still be visible to a later card's
        // match check, so newly-created prototypes are appended to this cache as they happen.
        val prototypeCache = db.cardPrototypeDao().getAllOnce().toMutableList()
        val abilityKeywordRegistry = db.abilityKeywordDao().getAllOnce()

        val total = pack.cards.size.coerceAtLeast(1)
        pack.cards.forEachIndexed { index, card ->
            onProgress(
                ImportProgress(
                    "Importing ${pack.set_language} ${pack.set_code}: card ${index + 1}/${pack.cards.size}",
                    (index + 1f) / total,
                )
            )

            val existingPrint = cardPrintDao.findByLanguageSetNumberCount(
                pack.set_language, pack.set_code, card.set_number, card.set_count
            )

            if (existingPrint == null) {
                val prototypeId = matchOrCreatePrototype(card, prototypeCache, abilityKeywordRegistry) { prototypesCreated++ }
                val printId = cardPrintDao.insertReturningId(
                    CardPrint(
                        cardPrototypeId = prototypeId,
                        language = pack.set_language,
                        set = pack.set_code,
                        setNumber = card.set_number,
                        setCount = card.set_count,
                        rarity = card.rarity,
                        translatedName = card.translated_name,
                        translatedText = card.translated_text,
                    )
                ).toInt()
                assetBytesFor(entries, pack, card)?.let { storeCardArt(printId, it) }
                touchedPrintIds.add(printId)
                cardsAdded++
                return@forEachIndexed
            }

            val conflict = conflictsByPrintId[existingPrint.id]
            if (conflict == null) {
                cardsSkipped++
                return@forEachIndexed
            }

            val resolution = resolutions[existingPrint.id] ?: ConflictResolution.TAKE_NEW
            if (resolution == ConflictResolution.KEEP_OLD) {
                cardsSkipped++
                return@forEachIndexed
            }

            val coreChanged = conflict.fieldDiffs.any { it.label in CORE_PROTOTYPE_FIELDS }
            val prototypeId = if (coreChanged) {
                matchOrCreatePrototype(card, prototypeCache, abilityKeywordRegistry) { prototypesCreated++ }
            } else {
                existingPrint.cardPrototypeId
            }

            cardPrintDao.update(
                existingPrint.copy(
                    cardPrototypeId = prototypeId,
                    rarity = card.rarity,
                    translatedName = card.translated_name,
                    translatedText = card.translated_text,
                )
            )

            if (conflict.assetDiff != null) {
                assetBytesFor(entries, pack, card)?.let { bytes ->
                    storeCardArt(existingPrint.id, bytes)
                    db.cardImageFeaturesDao().deleteByCardPrintIds(listOf(existingPrint.id))
                    touchedPrintIds.add(existingPrint.id)
                }
            }

            cardsUpdated++
        }

        db.cardSetMetaDao().insert(CardSetMeta(pack.set_language, pack.set_code, pack.set_name))
        refreshDynamicSetNames()

        onProgress(ImportProgress("Updating search index..."))
        backfillEmbeddings()
        onProgress(ImportProgress("Extending scan-match index..."))
        cardImageMatchRepository.indexPrints(touchedPrintIds)

        SetImportResult(
            setLanguage = pack.set_language,
            setCode = pack.set_code,
            setName = pack.set_name,
            cardsAdded = cardsAdded,
            cardsUpdated = cardsUpdated,
            cardsSkipped = cardsSkipped,
            prototypesCreated = prototypesCreated,
        )
    }

    /** Pack art is documented as PNG (specs/0003), but some hand-built packs ship JPG under the same base name - try both rather than silently dropping art. */
    private fun assetBytesFor(entries: Map<String, ByteArray>, pack: SetPackData, card: SetPackCard): ByteArray? {
        val base = "assets/${pack.set_language}_${pack.set_code}_${card.set_number}_${card.set_count}"
        return entries["$base.png"] ?: entries["$base.jpg"]
    }

    private fun readZipEntries(zipStream: InputStream): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(BufferedInputStream(zipStream)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return entries
    }

    private fun buildFieldDiffs(
        existingPrototype: CardPrototype?,
        existingPrint: CardPrint,
        card: SetPackCard,
    ): List<FieldDiff> {
        val diffs = mutableListOf<FieldDiff>()
        fun add(label: String, old: String?, new: String?) {
            if (old != new) diffs.add(FieldDiff(label, old, new))
        }
        add("Name", existingPrototype?.name, card.name)
        add("Civilization", existingPrototype?.civilization, card.civilization)
        add("Races", existingPrototype?.races, card.races)
        add("Mana", existingPrototype?.mana?.toString(), card.mana?.toString())
        add("Power", existingPrototype?.power, card.power)
        add("Type", existingPrototype?.type, card.type)
        add("Text", existingPrototype?.text, card.text)
        add("Rarity", existingPrint.rarity, card.rarity)
        add("Translated name", existingPrint.translatedName, card.translated_name)
        add("Translated text", existingPrint.translatedText, card.translated_text)
        return diffs
    }

    /** Null = no incoming art to compare (caller only invokes this when there is); see decisions/0012 for why this is a perceptual hash, not byte/pixel-exact. */
    private fun detectAssetDiff(existingPrintId: Int, incomingBytes: ByteArray): AssetDiffKind? {
        val incomingBitmap = BitmapFactory.decodeByteArray(incomingBytes, 0, incomingBytes.size) ?: return null
        try {
            val existingBitmap = try {
                CardImageStore.open(context, "card_images/$existingPrintId.jpg")?.use { BitmapFactory.decodeStream(it) }
            } catch (e: IOException) {
                null
            }
            if (existingBitmap == null) return AssetDiffKind.ADDED
            try {
                return if (ImageSimilarity.looksSame(existingBitmap, incomingBitmap)) null else AssetDiffKind.CHANGED
            } finally {
                existingBitmap.recycle()
            }
        } finally {
            incomingBitmap.recycle()
        }
    }

    /**
     * Matches [card] to an existing prototype in [prototypeCache] by exact tuple equality
     * after normalizing cosmetic formatting differences (decisions/0011, amended by specs/0007
     * to normalize before comparing rather than compare raw). No match creates a new prototype,
     * appended to [prototypeCache] so a later card in the same pack can still match it, and
     * parses [abilityKeywordRegistry] against its text once (never re-run later, see specs/0007).
     */
    private fun matchOrCreatePrototype(
        card: SetPackCard,
        prototypeCache: MutableList<CardPrototype>,
        abilityKeywordRegistry: List<AbilityKeyword>,
        onCreated: () -> Unit,
    ): Int {
        val existing = prototypeCache.find { identityMatches(it, card) }
        if (existing != null) return existing.id

        onCreated()
        val prototype = CardPrototype(
            name = card.name,
            civilization = card.civilization,
            races = card.races,
            mana = card.mana,
            power = card.power,
            type = card.type,
            text = card.text,
        )
        val prototypeId = db.cardPrototypeDao().insertReturningId(prototype).toInt()
        prototypeCache.add(prototype.copy(id = prototypeId))

        for (keyword in matchAbilityKeywords(card.text, abilityKeywordRegistry)) {
            db.abilityKeywordDao().insertAssociation(CardPrototypeAbilityKeyword(prototypeId, keyword.id))
        }

        return prototypeId
    }

    private fun identityMatches(existing: CardPrototype, card: SetPackCard): Boolean {
        return normalizeForIdentityMatch(existing.name) == normalizeForIdentityMatch(card.name) &&
            normalizeMultiValueForIdentityMatch(existing.civilization) == normalizeMultiValueForIdentityMatch(card.civilization) &&
            normalizeMultiValueForIdentityMatch(existing.races) == normalizeMultiValueForIdentityMatch(card.races) &&
            existing.mana == card.mana &&
            normalizeForIdentityMatch(existing.power) == normalizeForIdentityMatch(card.power) &&
            normalizeForIdentityMatch(existing.type) == normalizeForIdentityMatch(card.type) &&
            normalizeForIdentityMatch(existing.text) == normalizeForIdentityMatch(card.text)
    }

    private fun storeCardArt(printId: Int, imageBytes: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return
        try {
            CardImageStore.save(context, "card_images/$printId.jpg", bitmap)

            val scale = LOW_RES_WIDTH.toFloat() / bitmap.width
            val lowRes = Bitmap.createScaledBitmap(
                bitmap, LOW_RES_WIDTH, (bitmap.height * scale).toInt().coerceAtLeast(1), true
            )
            try {
                CardImageStore.save(context, "card_images_low/$printId.jpg", lowRes)
            } finally {
                if (lowRes != bitmap) lowRes.recycle()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun backfillEmbeddings() {
        val dao = db.cardPrototypeEmbeddingDao()
        for (prototype in dao.getPrototypesMissingEmbedding()) {
            val text = prototype.text?.let { stripReminderText(it) }?.takeIf { it.isNotBlank() } ?: continue
            val vector = semanticSearchEngine.embed(text)
            dao.insert(CardPrototypeEmbedding.fromFloatArray(prototype.id, vector))
        }
    }

    private suspend fun refreshDynamicSetNames() {
        val entries = db.cardSetMetaDao().getAll().first().map { (it.language to it.set) to it.name }
        CardSetOrder.loadDynamicNames(entries)
    }
}
