package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import ro.daydreamstalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardSetOrder
import java.io.File
import javax.inject.Inject

/**
 * One-time settings action that physically renumbers `CardPrint.id` to match
 * [CardSetOrder]'s canonical release order (EN, then DE, then other non-JP
 * languages alphabetically, then JP - each internally ordered by set release,
 * then set number), instead of the arrival order ids happen to have from
 * however sets were imported. Every place that stores a `CardPrint.id` -
 * `TransactedCard.cardPrintId`, `CardPrintUserData.cardPrintId`,
 * `CardImageFeatures.cardPrintId`, and the on-disk `card_images/{id}.jpg` /
 * `card_images_low/{id}.jpg` art files - is remapped through the same
 * old-id -> new-id bijection so nothing is orphaned or silently repointed at
 * the wrong print. See decisions/0019-card-print-id-canonical-reorder.md.
 */
class CardPrintReorderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: DuelMastersInventoryDatabase,
) {
    // Ids are shifted through this offset (guaranteed above any real id, since
    // SQLite INTEGER PRIMARY KEY / autoGenerate ids never approach it) as an
    // intermediate step, so a two-pass renumber never collides a still-unmoved
    // row with one that's already landed on its final id: every source id in
    // pass 1 lands in [OFFSET, OFFSET+N), disjoint from every target id in
    // pass 2's [1, N] range.
    private val offset = 1_000_000_000L

    /**
     * Computes the canonical order for every current [CardPrint], reassigns
     * `id` (and every dependent table's `cardPrintId`, and the on-disk art
     * files) to a dense 1..N sequence matching that order, and returns the
     * number of prints whose id actually changed.
     */
    suspend fun reorderToCanonicalOrder(): Int {
        val prints = db.cardPrintDao().getAll().first()

        val sorted = prints.sortedWith(
            compareBy(
                { CardSetOrder.languageBucket(it.language) },
                { if (CardSetOrder.languageBucket(it.language) == CardSetOrder.BUCKET_OTHER) it.language else "" },
                { CardSetOrder.setRank(it.set) },
                { it.setNumber.takeWhile(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE },
                { it.setNumber },
                { it.rarity ?: "" },
                { it.cardPrototypeId },
                { it.id },
            )
        )

        val mapping = sorted.mapIndexed { index, print -> print.id to (index + 1) }
            .filter { (oldId, newId) -> oldId != newId }

        if (mapping.isEmpty()) return 0

        renumberDatabaseRows(mapping)
        renumberArtFiles(mapping)

        return mapping.size
    }

    private fun renumberDatabaseRows(mapping: List<Pair<Int, Int>>) {
        val sqliteDb = db.openHelper.writableDatabase
        sqliteDb.beginTransaction()
        try {
            // Deferred so a mid-batch state where a CardPrintUserData row
            // temporarily points at a CardPrint id that doesn't exist yet
            // (between the two passes below) doesn't trip its FK constraint -
            // only checked again, and enforced, at commit.
            sqliteDb.execSQL("PRAGMA defer_foreign_keys=1")

            // PK tables: two-pass shift through the high offset range to avoid
            // colliding one row's target id with another row's not-yet-moved
            // current id.
            for (table in listOf("CardPrint", "CardPrintUserData", "CardImageFeatures")) {
                val idColumn = if (table == "CardPrint") "id" else "cardPrintId"
                val toOffset = sqliteDb.compileStatement("UPDATE `$table` SET `$idColumn` = ? WHERE `$idColumn` = ?")
                for ((oldId, _) in mapping) {
                    toOffset.bindLong(1, oldId + offset)
                    toOffset.bindLong(2, oldId.toLong())
                    toOffset.executeUpdateDelete()
                }
                val toFinal = sqliteDb.compileStatement("UPDATE `$table` SET `$idColumn` = ? WHERE `$idColumn` = ?")
                for ((oldId, newId) in mapping) {
                    toFinal.bindLong(1, newId.toLong())
                    toFinal.bindLong(2, oldId + offset)
                    toFinal.executeUpdateDelete()
                }
            }

            // Not a PK/unique column here - many TransactedCard rows can
            // share a cardPrintId, so a direct single-pass remap is safe.
            val updateTransacted = sqliteDb.compileStatement(
                "UPDATE `TransactedCard` SET `cardPrintId` = ? WHERE `cardPrintId` = ?"
            )
            for ((oldId, newId) in mapping) {
                updateTransacted.bindLong(1, newId.toLong())
                updateTransacted.bindLong(2, oldId.toLong())
                updateTransacted.executeUpdateDelete()
            }

            val fkCheck = sqliteDb.query("PRAGMA foreign_key_check")
            fkCheck.use {
                check(it.count == 0) { "Card print reorder left ${it.count} foreign key violation(s) in the database" }
            }

            sqliteDb.setTransactionSuccessful()
        } finally {
            sqliteDb.endTransaction()
        }
    }

    private fun renumberArtFiles(mapping: List<Pair<Int, Int>>) {
        for (dir in listOf("card_images", "card_images_low")) {
            for ((oldId, _) in mapping) {
                renameIfExists("$dir/$oldId.jpg", "$dir/${oldId + offset}.jpg")
            }
            for ((oldId, newId) in mapping) {
                renameIfExists("$dir/${oldId + offset}.jpg", "$dir/$newId.jpg")
            }
        }
    }

    private fun renameIfExists(fromRelativePath: String, toRelativePath: String) {
        val from = File(context.filesDir, fromRelativePath)
        if (!from.exists()) return
        val to = File(context.filesDir, toRelativePath)
        to.parentFile?.mkdirs()
        from.renameTo(to)
    }
}
