package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import ro.daydreamstalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardImageFeatures
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.CardImageStore
import ro.daydreamstalgia.duelmastersinventory.shared.utils.image.OrbMatcher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @param score ratio-test survivor count between the capture and this
 * print's reference art ([OrbMatcher.MatchResult.goodMatchCount]) - the
 * [MIN_MATCH_COUNT] accept/reject gate and the ranking key among candidates.
 * @param confidencePercent 0-100, what share of [score]'s ratio-test matches
 * also fit one consistent RANSAC homography
 * ([OrbMatcher.MatchResult.inlierCount] / [score]). Validated against the
 * `DuelMastersFoilReconstruct/data/extracted` handheld-photo ground truth
 * (decisions/0009 update, "Garkago Dragon still not identified"): the
 * correct print scores 83-98% confidence there, while the best-scoring wrong
 * print across the *entire* ~1100-print catalog never clears [MIN_MATCH_COUNT]
 * in the first place. Not a calibrated probability, but a real geometric
 * consistency measure now, not a flat count-based guess.
 */
data class CardImageMatch(val cardPrintId: Int, val score: Int, val confidencePercent: Int)

/**
 * Ratio-test matches needed before a candidate is even considered - well
 * above what any wrong print reaches (best observed: 18, scanning the whole
 * catalog against real handheld ground-truth photos) and well below what the
 * correct print reaches (worst observed: 66). See decisions/0009 update.
 */
private const val MIN_MATCH_COUNT = 25

/**
 * Matches scoring below this are dropped before ever reaching the UI - a
 * result the user can't trust isn't better than no result. Distinct from
 * [MIN_MATCH_COUNT], which only gates "was a card detected at all" the way
 * card_match.py uses it.
 */
private const val MIN_CONFIDENCE_PERCENT = 70

@Singleton
class CardImageMatchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: DuelMastersInventoryDatabase,
) {
    private val dao = db.cardImageFeaturesDao()

    init {
        if (!OpenCVLoader.initLocal()) {
            Log.w("CardImageMatchRepository", "OpenCVLoader.initLocal() failed")
        }
    }

    /** Builds the ORB descriptor index once, from `card_images/{cardPrintId}.jpg`. Safe to call repeatedly. */
    suspend fun ensureIndexBuilt() = withContext(Dispatchers.IO) {
        if (dao.count() > 0) return@withContext

        val printIds = db.cardPrintDao().getAll().first().map { it.id }
        indexPrints(printIds)
    }

    /** Extends the ORB descriptor index to cover [printIds] not indexed yet - used after a set-pack import instead of rescanning the whole catalog. Safe to call with already-indexed ids (they're skipped). */
    suspend fun indexPrints(printIds: List<Int>) = withContext(Dispatchers.IO) {
        val alreadyIndexed = dao.getAll().map { it.cardPrintId }.toSet()

        val features = printIds.filter { it !in alreadyIndexed }.mapNotNull { printId ->
            val bitmap = loadReferenceBitmap(printId) ?: return@mapNotNull null
            val computed = OrbMatcher.compute(bitmap)
            bitmap.recycle()
            if (computed.keypointCount == 0) {
                computed.release()
                return@mapNotNull null
            }
            val descriptorBytes = OrbMatcher.serialize(computed.descriptors)
            val keypointBytes = OrbMatcher.serializeKeypoints(computed.keypoints)
            computed.release()
            CardImageFeatures(
                cardPrintId = printId,
                descriptors = descriptorBytes,
                keypoints = keypointBytes,
                keypointCount = computed.keypointCount,
            )
        }

        dao.insertAll(features)
    }

    /**
     * Matches [bitmap] against every indexed print's descriptors. Results are
     * grouped by prototype rather than returned as a flat print list: the
     * [prototypeLimit] best-guess prototypes are kept (ranked by their
     * best-matching print's score), and every known print of each is
     * included, ordered by its own score. Prints scoring below
     * [MIN_MATCH_COUNT] or below [MIN_CONFIDENCE_PERCENT] are dropped before
     * grouping - the latter is what keeps a 38%-confidence guess off the
     * confirm sheet entirely instead of showing it as a low-trust option.
     *
     * @param allowedPrintIds when non-null, only these prints are scanned - used by the scan
     * screen's set filter to narrow the index before doing any ORB matching work, rather than
     * matching against the whole catalog and discarding results after the fact. Null (the
     * default) means unfiltered, matching every indexed print.
     */
    suspend fun findMatches(
        bitmap: Bitmap,
        prototypeLimit: Int = 3,
        allowedPrintIds: Set<Int>? = null,
    ): List<CardImageMatch> = withContext(Dispatchers.Default) {
        val prototypeByPrintId = db.cardPrintDao().getAll().first().associate { it.id to it.cardPrototypeId }
        val indexed = dao.getAll().let { entries ->
            if (allowedPrintIds == null) entries else entries.filter { it.cardPrintId in allowedPrintIds }
        }

        val query = OrbMatcher.compute(bitmap)
        if (query.keypointCount == 0) {
            query.release()
            return@withContext emptyList()
        }

        val perPrint = indexed.map { entry ->
            val referenceDescriptors = OrbMatcher.deserialize(entry.descriptors, entry.keypointCount)
            val referencePoints = OrbMatcher.deserializeKeypoints(entry.keypoints, entry.keypointCount)
            val result = OrbMatcher.match(query.descriptors, query.keypoints, referenceDescriptors, referencePoints)
            referenceDescriptors.release()
            val confidence = if (result.goodMatchCount > 0) {
                ((result.inlierCount.toDouble() / result.goodMatchCount) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }
            CardImageMatch(entry.cardPrintId, result.goodMatchCount, confidence)
        }
        query.release()

        perPrint
            .filter { it.score >= MIN_MATCH_COUNT && it.confidencePercent >= MIN_CONFIDENCE_PERCENT }
            .groupBy { prototypeByPrintId[it.cardPrintId] }
            .filterKeys { it != null }
            .values
            .sortedByDescending { prints -> prints.maxOf { it.score } }
            .take(prototypeLimit)
            .flatMap { prints -> prints.sortedByDescending { it.score } }
    }

    /** Redraws the ORB match between [query] (a capture frame) and [cardPrintId]'s reference art, for the confirm sheet's "matched keypoints" view. Recomputes rather than reusing index state - only ever called for the one currently-displayed candidate, not the whole index. */
    suspend fun visualizeMatch(query: Bitmap, cardPrintId: Int): Bitmap? = withContext(Dispatchers.Default) {
        val reference = loadReferenceBitmap(cardPrintId) ?: return@withContext null
        val result = OrbMatcher.drawGoodMatches(query, reference)
        reference.recycle()
        result
    }

    private fun loadReferenceBitmap(cardPrintId: Int): Bitmap? {
        return try {
            CardImageStore.open(context, "card_images/$cardPrintId.jpg")?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: java.io.IOException) {
            null
        }
    }
}
