package ro.ddnostalgia.duelmastersinventory.shared.data

import androidx.room.Database
import androidx.room.RoomDatabase
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.dao.ActorAliasDao
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.dao.ActorDao
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.model.Actor
import ro.ddnostalgia.duelmastersinventory.shared.data.actors.model.ActorAlias
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.dao.*
import ro.ddnostalgia.duelmastersinventory.shared.data.cards.model.*
import ro.ddnostalgia.duelmastersinventory.shared.data.export_sheet_configs.dao.ExportSheetConfigDao
import ro.ddnostalgia.duelmastersinventory.shared.data.export_sheet_configs.model.ExportSheetConfig
import ro.ddnostalgia.duelmastersinventory.shared.data.search.dao.CardPrototypeEmbeddingDao
import ro.ddnostalgia.duelmastersinventory.shared.data.search.model.CardPrototypeEmbedding
import ro.ddnostalgia.duelmastersinventory.shared.data.statistics.dao.StatisticsDao
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.dao.*
import ro.ddnostalgia.duelmastersinventory.shared.data.transactions.model.*

@Database(
    entities = [
        CardPrototype::class,
        CardPrint::class,
        CardPrintUserData::class,
        CardImageFeatures::class,

        Transaction::class,
        TransactedCard::class,

        Actor::class,
        ActorAlias::class,

        ExportSheetConfig::class,

        CardPrototypeEmbedding::class,

        CardSetMeta::class,

        AbilityKeyword::class,
        CardPrototypeAbilityKeyword::class,
    ],
    version = 19,
    exportSchema = false
)
abstract class DuelMastersInventoryDatabase: RoomDatabase() {
    abstract fun cardPrototypeDao(): CardPrototypeDao
    abstract fun cardPrintDao(): CardPrintDao
    abstract fun cardImageFeaturesDao(): CardImageFeaturesDao

    abstract fun transactionDao(): TransactionDao

    abstract fun transactedCardDao(): TransactedCardDao

    abstract fun actorDao(): ActorDao

    abstract fun actorAliasDao(): ActorAliasDao

    abstract fun exportSheetConfigDao(): ExportSheetConfigDao

    abstract fun statisticsDao(): StatisticsDao

    abstract fun genericDao(): GenericDao

    abstract fun cardPrototypeEmbeddingDao(): CardPrototypeEmbeddingDao

    abstract fun cardSetMetaDao(): CardSetMetaDao

    abstract fun abilityKeywordDao(): AbilityKeywordDao

}