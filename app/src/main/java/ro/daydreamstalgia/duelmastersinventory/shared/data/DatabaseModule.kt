package ro.daydreamstalgia.duelmastersinventory.shared.data

import android.app.Application
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardSetOrder
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardImageMatchRepository
import ro.daydreamstalgia.duelmastersinventory.shared.data.search.SemanticSearchEngine
import ro.daydreamstalgia.duelmastersinventory.shared.data.search.model.CardPrototypeEmbedding
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.FeatureFlags
import ro.daydreamstalgia.duelmastersinventory.shared.utils.text.stripReminderText
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    fun provideGenericDao(db: DuelMastersInventoryDatabase): GenericDao {
        return db.genericDao()
    }

    @Provides
    @Singleton
    fun provideDatabase(app: Application): DuelMastersInventoryDatabase =
        Room.databaseBuilder(app, DuelMastersInventoryDatabase::class.java, "dminventory.db")
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20)
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)

                    Log.d("DatabaseModule", "Populating database")

                    ioThread {
                        val database = Room.databaseBuilder(
                            app,
                            DuelMastersInventoryDatabase::class.java,
                            "dminventory.db"
                        ).build()
                        val cardPrototypeDao = database.cardPrototypeDao()

                        // No bundled catalog seeding - specs/0006 removed all hardcoded
                        // CSV/image assets. A fresh install starts with zero prototypes/prints
                        // until the first-run onboarding flow (or, later, the Sets screen's
                        // Discover/filesystem actions) imports a set pack.

                        val cardPrototypeEmbeddingDao = database.cardPrototypeEmbeddingDao()
                        if (cardPrototypeEmbeddingDao.count() == 0) {
                            Log.d("DatabaseModule", "Building semantic search index")
                            val startedAt = System.currentTimeMillis()

                            val semanticSearchEngine = SemanticSearchEngine(app)
                            val allPrototypes = runBlocking { cardPrototypeDao.getAll().first() }
                            Log.d("DatabaseModule", "Fetched ${allPrototypes.size} prototypes to embed")

                            var embedded = 0
                            for ((i, prototype) in allPrototypes.withIndex()) {
                                // Ability rules text only - name/races are deliberately
                                // excluded. Mixing them in diluted the signal and caused
                                // spurious matches driven by shared name substrings (e.g.
                                // a query mentioning "turn" matching a card named
                                // "Turnip" purely on subword overlap). Name-based typo
                                // tolerance is handled separately by the fuzzy matcher
                                // against CardPrototype.name in CardPrototypeRepository.
                                // Reminder text (e.g. "Shield trigger (When this spell is
                                // put into your hand from your shield zone...)") is nearly
                                // identical across a huge share of the card pool, so it's
                                // stripped too - otherwise every shield-trigger card
                                // clusters together for any shield/hand-related query
                                // regardless of what its actual unique effect is.
                                //
                                // Vanilla cards with no rules text at all are skipped
                                // entirely (no embedding row) rather than falling back to
                                // embedding the card's name - that fallback previously
                                // reintroduced the exact name-collision noise this whole
                                // scheme exists to avoid, since a proper noun's embedding
                                // is mostly meaningless subword soup that ends up looking
                                // spuriously similar to OTHER proper-noun-only embeddings.
                                // No row means CardPrototypeRepository.scoreCard treats it
                                // as semanticScore=0, which is correct: a card with no
                                // ability text has nothing for a natural-language query to
                                // semantically match.
                                val text = prototype.text
                                    ?.let { stripReminderText(it) }
                                    ?.takeIf { it.isNotBlank() }
                                    ?: continue

                                val vector = semanticSearchEngine.embed(text)
                                cardPrototypeEmbeddingDao.insert(
                                    CardPrototypeEmbedding.fromFloatArray(prototype.id, vector)
                                )
                                embedded++

                                if (i % 50 == 0) {
                                    Log.d("DatabaseModule", "Embedded $embedded/$i processed of ${allPrototypes.size} (${System.currentTimeMillis() - startedAt}ms elapsed)")
                                }
                            }

                            Log.d("DatabaseModule", "Semantic search index built in ${System.currentTimeMillis() - startedAt}ms")
                        }

                        val cardImageMatchRepository = CardImageMatchRepository(app, database)
                        if (FeatureFlags.CARD_SCAN && database.cardImageFeaturesDao().count() == 0) {
                            Log.d("DatabaseModule", "Building card scan ORB feature index")
                            val startedAt = System.currentTimeMillis()
                            runBlocking { cardImageMatchRepository.ensureIndexBuilt() }
                            Log.d("DatabaseModule", "Card scan ORB feature index built in ${System.currentTimeMillis() - startedAt}ms")
                        }

                        val dynamicSetNames = runBlocking { database.cardSetMetaDao().getAll().first() }
                            .map { (it.language to it.set) to it.name }
                        CardSetOrder.loadDynamicNames(dynamicSetNames)

                        database.close()
                    }

                }
            })
            .build()
}

fun ioThread(f: () -> Unit) = Thread(f).start()

