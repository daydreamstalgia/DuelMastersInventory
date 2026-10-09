package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ro.daydreamstalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardImageFeatures
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrint
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrintUserData
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrototype
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.Transaction
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.TransactedCard
import ro.daydreamstalgia.duelmastersinventory.shared.data.transactions.model.TransactionType
import java.io.File
import java.time.LocalDate

/**
 * Verifies the print-id reorder tool (see decisions/0019 in the KB): every
 * print ends up on a dense 1..N id matching [ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardSetOrder]'s
 * canonical order, every table/file that stored the old id follows the same
 * row to its new id (not just gets some arbitrary new id), and the database
 * is left with zero foreign-key violations.
 */
@RunWith(AndroidJUnit4::class)
class CardPrintReorderRepositoryTest {

    private lateinit var db: DuelMastersInventoryDatabase
    private lateinit var repository: CardPrintReorderRepository
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, DuelMastersInventoryDatabase::class.java).build()
        repository = CardPrintReorderRepository(context, db)

        File(context.filesDir, "card_images").deleteRecursively()
        File(context.filesDir, "card_images_low").deleteRecursively()
    }

    @After
    fun tearDown() {
        db.close()
        File(context.filesDir, "card_images").deleteRecursively()
        File(context.filesDir, "card_images_low").deleteRecursively()
    }

    // Identifies a print by content rather than by its (about to change) id.
    private data class PrintIdentity(val language: String, val set: String, val setNumber: String)

    private fun CardPrint.identity() = PrintIdentity(language, set, setNumber)

    @Test
    fun reorder_assignsDenseCanonicalIds_andRemapsAllDependents() = runBlocking {
        db.cardPrototypeDao().insert(
            CardPrototype(id = 1, name = "Test", civilization = "Fire", races = "Human", mana = 1, power = "1000", type = "Creature", text = null)
        )

        // Deliberately scrambled insertion order/ids relative to canonical order.
        val jpDm01 = CardPrint(id = 50, cardPrototypeId = 1, language = "JP", set = "DM-01", setNumber = "3", setCount = "110", rarity = "C")
        val enDm02 = CardPrint(id = 10, cardPrototypeId = 1, language = "EN", set = "DM-02", setNumber = "7", setCount = "110", rarity = "C")
        val deDm01 = CardPrint(id = 30, cardPrototypeId = 1, language = "DE", set = "DM-01", setNumber = "2", setCount = "110", rarity = "C")
        val enDm01High = CardPrint(id = 5, cardPrototypeId = 1, language = "EN", set = "DM-01", setNumber = "10", setCount = "110", rarity = "C")
        val enDm01Low = CardPrint(id = 99, cardPrototypeId = 1, language = "EN", set = "DM-01", setNumber = "1", setCount = "110", rarity = "C")
        val frDm01 = CardPrint(id = 20, cardPrototypeId = 1, language = "FR", set = "DM-01", setNumber = "1", setCount = "110", rarity = "C")

        val allPrints = listOf(jpDm01, enDm02, deDm01, enDm01High, enDm01Low, frDm01)
        for (print in allPrints) db.cardPrintDao().insert(print)

        // Dependent rows keyed on the pre-reorder ids.
        db.cardPrintDao().insertOrUpdateUserData(CardPrintUserData(cardPrintId = enDm01Low.id, wishlist = true))
        db.cardImageFeaturesDao().insertAll(
            listOf(CardImageFeatures(cardPrintId = frDm01.id, descriptors = byteArrayOf(1, 2, 3), keypoints = byteArrayOf(4, 5), keypointCount = 1))
        )
        val transactionId = db.transactionDao().insert(
            Transaction(type = TransactionType.INBOUND, actorId = insertActor(), channel = null, date = LocalDate.now(), costEuro = 0.0, description = "test")
        ).toInt()
        db.transactedCardDao().insertAll(
            listOf(
                TransactedCard(cardPrintId = enDm02.id, inTransactionId = transactionId),
                TransactedCard(cardPrintId = enDm02.id, inTransactionId = transactionId),
            )
        )

        // Art files for two of the prints, so the rename pass is exercised too.
        writeArt("card_images/${enDm01Low.id}.jpg", "enDm01Low-full")
        writeArt("card_images_low/${enDm01Low.id}.jpg", "enDm01Low-thumb")
        writeArt("card_images/${frDm01.id}.jpg", "frDm01-full")

        val changed = repository.reorderToCanonicalOrder()
        assertEquals(allPrints.size, changed)

        val reordered = db.cardPrintDao().getAll().first()
        val ids = reordered.map { it.id }.sorted()
        assertEquals((1..allPrints.size).toList(), ids)

        // Canonical order: EN DM-01 (setNumber 1, then 10), EN DM-02, DE DM-01, FR DM-01, JP DM-01.
        val byIdentity = reordered.associateBy { it.identity() }
        val newIdOf = { p: CardPrint -> byIdentity.getValue(p.identity()).id }

        assertTrue(newIdOf(enDm01Low) < newIdOf(enDm01High))
        assertTrue(newIdOf(enDm01High) < newIdOf(enDm02))
        assertTrue(newIdOf(enDm02) < newIdOf(deDm01))
        assertTrue(newIdOf(deDm01) < newIdOf(frDm01))
        assertTrue(newIdOf(frDm01) < newIdOf(jpDm01))

        // Dependents followed their print to its new id, not some other print's.
        val newWishlistRow = db.cardPrintDao().getUserDataById(newIdOf(enDm01Low)).first()
        assertTrue(newWishlistRow?.wishlist == true)

        val newFeatures = db.cardImageFeaturesDao().getAll()
        assertEquals(1, newFeatures.size)
        assertEquals(newIdOf(frDm01), newFeatures[0].cardPrintId)

        val transactedCardPrintIds = mutableListOf<Int>()
        db.openHelper.writableDatabase.query("SELECT cardPrintId FROM TransactedCard").use { cursor ->
            while (cursor.moveToNext()) transactedCardPrintIds.add(cursor.getInt(0))
        }
        assertEquals(2, transactedCardPrintIds.size)
        assertTrue(transactedCardPrintIds.all { it == newIdOf(enDm02) })

        assertTrue(readArt("card_images/${newIdOf(enDm01Low)}.jpg") == "enDm01Low-full")
        assertTrue(readArt("card_images_low/${newIdOf(enDm01Low)}.jpg") == "enDm01Low-thumb")
        assertTrue(readArt("card_images/${newIdOf(frDm01)}.jpg") == "frDm01-full")

        val fkViolations = db.openHelper.writableDatabase.query("PRAGMA foreign_key_check")
        fkViolations.use { assertEquals(0, it.count) }

        // Idempotent: already-canonical order means nothing left to change.
        assertEquals(0, repository.reorderToCanonicalOrder())
    }

    private suspend fun insertActor(): Int {
        return db.actorDao().insert(
            ro.daydreamstalgia.duelmastersinventory.shared.data.actors.model.Actor(firstName = "Test", lastName = null)
        ).toInt()
    }

    private fun writeArt(relativePath: String, content: String) {
        val file = File(context.filesDir, relativePath)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    private fun readArt(relativePath: String): String? {
        val file = File(context.filesDir, relativePath)
        return if (file.exists()) file.readText() else null
    }
}
