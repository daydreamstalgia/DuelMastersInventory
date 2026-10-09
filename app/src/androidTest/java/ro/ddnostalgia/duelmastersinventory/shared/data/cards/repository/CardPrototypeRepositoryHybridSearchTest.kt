package ro.ddnostalgia.duelmastersinventory.shared.data.cards.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ro.ddnostalgia.duelmastersinventory.shared.data.DuelMastersInventoryDatabase
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrint
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.CardPrototype
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.FilterMode
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.PrototypePrintsFilterParams
import ro.ddnostalgia.duelmastersinventory.shared.data.search.SemanticSearchEngine
import ro.ddnostalgia.duelmastersinventory.shared.data.search.model.CardPrototypeEmbedding
import ro.ddnostalgia.duelmastersinventory.shared.utils.text.stripReminderText

/**
 * Exercises the hybrid search ranking end-to-end (real bundled MiniLM model +
 * real tokenizer + an in-memory Room DB), covering the three signals it blends:
 * substring hit, fuzzy name typo, and semantic paraphrase.
 */
@RunWith(AndroidJUnit4::class)
class CardPrototypeRepositoryHybridSearchTest {

    private lateinit var db: DuelMastersInventoryDatabase
    private lateinit var repository: CardPrototypeRepository
    private lateinit var engine: SemanticSearchEngine

    private val natasha = CardPrototype(
        id = 1, name = "Natasha, Fire Whip", civilization = "Fire", races = "Human",
        mana = 3, power = "2000", type = "Creature", text = "Speed attacker."
    )
    private val shieldCard = CardPrototype(
        id = 2, name = "Aqua Surfer", civilization = "Water", races = "Liquid People",
        mana = 2, power = "1000", type = "Creature",
        text = "When this creature enters the battlefield, put a shield card into your hand."
    )
    private val unrelatedCard = CardPrototype(
        id = 3, name = "Bolshack Dragon", civilization = "Fire", races = "Dragon",
        mana = 3, power = "6000", type = "Creature", text = "Destroy one of your opponent's creatures."
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, DuelMastersInventoryDatabase::class.java).build()
        engine = SemanticSearchEngine(context)
        repository = CardPrototypeRepository(db, engine)

        for (prototype in listOf(natasha, shieldCard, unrelatedCard)) {
            db.cardPrototypeDao().insert(prototype)
            db.cardPrintDao().insert(
                CardPrint(
                    cardPrototypeId = prototype.id,
                    language = "en",
                    set = "TEST",
                    setNumber = prototype.id.toString(),
                    setCount = "3",
                    rarity = "C"
                )
            )

            // Mirrors DatabaseModule's real seeding logic exactly (text-only,
            // reminder-text stripped) - this test previously embedded
            // name+races+text, which stopped matching production and made it a
            // misleading regression check.
            val text = stripReminderText(prototype.text.orEmpty())
            db.cardPrototypeEmbeddingDao().insert(
                CardPrototypeEmbedding.fromFloatArray(prototype.id, engine.embed(text))
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun typoedName_matchesViaFuzzyScore() = runBlocking {
        val results = search("Nastasha")

        assertTrue(results.any { it.prototype.id == natasha.id })
        assertFalse(results.any { it.prototype.id == unrelatedCard.id })
    }

    @Test
    fun paraphrasedAbility_matchesViaSemanticScore() = runBlocking {
        val results = search("put shield in hand")

        assertTrue(results.any { it.prototype.id == shieldCard.id })
        assertFalse(results.any { it.prototype.id == unrelatedCard.id })
    }

    @Test
    fun exactSubstring_alwaysMatches() = runBlocking {
        val results = search("Bolshack")

        assertTrue(results.any { it.prototype.id == unrelatedCard.id })
    }

    private suspend fun search(query: String) = repository.filterCardPrototypeWithPrintsAndOwnedCount(
        PrototypePrintsFilterParams(search = query),
        FilterMode.STRICT
    ).first()
}
