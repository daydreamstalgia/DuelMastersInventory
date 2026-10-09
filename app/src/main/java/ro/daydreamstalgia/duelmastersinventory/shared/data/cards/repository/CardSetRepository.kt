package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ro.daydreamstalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardSetOrder
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.setimport.CardImageStore
import javax.inject.Inject
import javax.inject.Singleton

/** One row of the Sets screen - a `(language, set)` pair with its print/ownership counts. */
data class CardSetSummary(
    val language: String,
    val setCode: String,
    val displayName: String?,
    val totalPrints: Int,
    val ownedCount: Int,
    val transactedCount: Int,
    // Has a CardSetMeta row - i.e. came from a set-pack import rather than the bundled CSV
    // seed. Only these are ever offered for deletion, see decisions/0012.
    val isDynamic: Boolean,
)

/**
 * Sets screen data access: listing every loaded `(language, set)` with its counts, and deleting
 * a dynamically-imported one (CardPrint/CardSetMeta/CardImageFeatures rows + stored art files).
 * See specs/0004-sets-management-and-reimport-conflicts.md and decisions/0012.
 */
@Singleton
class CardSetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: DuelMastersInventoryDatabase,
) {
    sealed interface DeleteResult {
        data object Deleted : DeleteResult
        data class Blocked(val transactedCount: Int) : DeleteResult
    }

    fun getSetSummaries(): Flow<List<CardSetSummary>> = combine(
        db.cardPrintDao().getSetSummaries(),
        db.cardSetMetaDao().getAll(),
    ) { rows, metas ->
        val dynamicKeys = metas.map { it.language to it.set }.toSet()
        rows.map { row ->
            CardSetSummary(
                language = row.language,
                setCode = row.setCode,
                displayName = CardSetOrder.displayName(row.language, row.setCode),
                totalPrints = row.totalPrints,
                ownedCount = row.ownedCount,
                transactedCount = row.transactedCount,
                isDynamic = (row.language to row.setCode) in dynamicKeys,
            )
        }.sortedWith(compareBy(CardSetOrder.comparator) { it.language to it.setCode })
    }

    suspend fun deleteSet(language: String, set: String): DeleteResult = withContext(Dispatchers.IO) {
        val printIds = db.cardPrintDao().getIdsByLanguageSet(language, set)
        if (printIds.isNotEmpty()) {
            val transactedCount = db.transactedCardDao().countByCardPrintIds(printIds)
            if (transactedCount > 0) return@withContext DeleteResult.Blocked(transactedCount)
        }

        db.withTransaction {
            db.cardImageFeaturesDao().deleteByCardPrintIds(printIds)
            db.cardPrintDao().deleteByLanguageSet(language, set)
            db.cardSetMetaDao().deleteByLanguageSet(language, set)
        }

        for (printId in printIds) {
            CardImageStore.delete(context, "card_images/$printId.jpg")
            CardImageStore.delete(context, "card_images_low/$printId.jpg")
        }

        refreshDynamicSetNames()
        DeleteResult.Deleted
    }

    private suspend fun refreshDynamicSetNames() {
        val entries = db.cardSetMetaDao().getAll().first().map { (it.language to it.set) to it.name }
        CardSetOrder.loadDynamicNames(entries)
    }
}
