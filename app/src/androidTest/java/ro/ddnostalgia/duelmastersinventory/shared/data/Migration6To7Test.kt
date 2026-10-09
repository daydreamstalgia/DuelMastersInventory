package ro.ddnostalgia.duelmastersinventory.shared.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises MIGRATION_6_7 directly against a real SQLite file containing only
 * the pre-migration tables the migration actually reads/rewrites (`Transaction`,
 * `TransactedCard`) with data shaped like real usage: a self-referencing trade
 * pair, a TransactedCard spanning two transactions, and a repeated legacy actor
 * string that must collapse into a single Actor row.
 */
@RunWith(AndroidJUnit4::class)
class Migration6To7Test {
    private val dbName = "migration-6-7-test.db"
    private lateinit var context: Context

    @After
    fun cleanup() {
        if (::context.isInitialized) context.deleteDatabase(dbName)
    }

    private fun openDatabase(): SupportSQLiteDatabase {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(dbName)

        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        return FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase
    }

    @Test
    fun migration_preservesTransactionsAndBackfillsActors() {
        val db = openDatabase()

        db.execSQL(
            """
            CREATE TABLE `Transaction` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `type` TEXT NOT NULL,
                `actor` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `costEuro` REAL NOT NULL,
                `description` TEXT NOT NULL,
                `tradeReferenceId` INTEGER,
                `parcelTrackingNumber` TEXT,
                FOREIGN KEY(`tradeReferenceId`) REFERENCES `Transaction`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX `index_Transaction_tradeReferenceId` ON `Transaction` (`tradeReferenceId`)")

        db.execSQL(
            """
            CREATE TABLE `TransactedCard` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `cardPrintId` INTEGER NOT NULL,
                `inTransactionId` INTEGER NOT NULL,
                `outTransactionId` INTEGER,
                `condition` TEXT,
                FOREIGN KEY(`inTransactionId`) REFERENCES `Transaction`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`outTransactionId`) REFERENCES `Transaction`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX `index_TransactedCard_inTransactionId` ON `TransactedCard` (`inTransactionId`)")
        db.execSQL("CREATE INDEX `index_TransactedCard_outTransactionId` ON `TransactedCard` (`outTransactionId`)")

        // Two transactions share the same legacy actor string; one trade pair
        // (self FK both ways); one TransactedCard spanning transactions 1 and 2.
        db.execSQL(
            "INSERT INTO `Transaction` (id, type, actor, date, costEuro, description, tradeReferenceId, parcelTrackingNumber) VALUES " +
                "(1, 'INBOUND', 'John Doe', '2024-01-01', 10.0, 'first', NULL, NULL)"
        )
        db.execSQL(
            "INSERT INTO `Transaction` (id, type, actor, date, costEuro, description, tradeReferenceId, parcelTrackingNumber) VALUES " +
                "(2, 'OUTBOUND', 'ebay seller xyz', '2024-01-02', 5.0, 'second', 3, NULL)"
        )
        db.execSQL(
            "INSERT INTO `Transaction` (id, type, actor, date, costEuro, description, tradeReferenceId, parcelTrackingNumber) VALUES " +
                "(3, 'INBOUND', 'John Doe', '2024-01-03', 5.0, 'third', 2, NULL)"
        )
        db.execSQL(
            "INSERT INTO `TransactedCard` (id, cardPrintId, inTransactionId, outTransactionId, condition) VALUES " +
                "(1, 100, 1, 2, 'mint')"
        )

        MIGRATION_6_7.migrate(db)

        // Row count and ids preserved.
        db.query("SELECT id FROM `Transaction` ORDER BY id").use { c ->
            assertEquals(3, c.count)
        }

        // The two distinct legacy actor strings collapse into exactly 2 Actor rows,
        // each with exactly one 'legacy' alias carrying the original text.
        db.query("SELECT COUNT(*) FROM `Actor`").use { c -> c.moveToFirst(); assertEquals(2, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM `ActorAlias` WHERE platform = 'legacy'").use { c ->
            c.moveToFirst(); assertEquals(2, c.getInt(0))
        }

        var johnActorId = -1
        db.query("SELECT actorId FROM `ActorAlias` WHERE username = 'John Doe'").use { c ->
            assertTrue(c.moveToFirst())
            johnActorId = c.getInt(0)
        }
        db.query("SELECT actorId FROM `Transaction` WHERE id IN (1, 3)").use { c ->
            while (c.moveToNext()) assertEquals(johnActorId, c.getInt(0))
        }

        // tradeReferenceId self-links survived the rebuild.
        db.query("SELECT tradeReferenceId FROM `Transaction` WHERE id = 2").use { c ->
            c.moveToFirst(); assertEquals(3, c.getInt(0))
        }
        db.query("SELECT tradeReferenceId FROM `Transaction` WHERE id = 3").use { c ->
            c.moveToFirst(); assertEquals(2, c.getInt(0))
        }

        // TransactedCard's in/out transaction references still resolve.
        db.query("SELECT inTransactionId, outTransactionId FROM `TransactedCard` WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
            assertEquals(2, c.getInt(1))
        }

        // No dangling foreign keys anywhere in the migrated database.
        db.query("PRAGMA foreign_key_check").use { c -> assertEquals(0, c.count) }

        db.close()
    }
}
