package ro.daydreamstalgia.duelmastersinventory.shared.data

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
 * Exercises MIGRATION_19_20: Transaction.actorId becomes nullable, existing rows (and their
 * actor links) survive the table rebuild unchanged.
 */
@RunWith(AndroidJUnit4::class)
class Migration19To20Test {
    private val dbName = "migration-19-20-test.db"
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
    fun migration_makesActorIdNullable_andKeepsRows() {
        val db = openDatabase()

        db.execSQL("CREATE TABLE `Actor` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `firstName` TEXT, `lastName` TEXT)")
        db.execSQL(
            """
            CREATE TABLE `Transaction` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `type` TEXT NOT NULL,
                `actorId` INTEGER NOT NULL,
                `channel` TEXT,
                `date` TEXT NOT NULL,
                `costEuro` REAL NOT NULL,
                `description` TEXT NOT NULL,
                `tradeReferenceId` INTEGER,
                `parcelTrackingNumber` TEXT,
                FOREIGN KEY(`tradeReferenceId`) REFERENCES `Transaction`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`actorId`) REFERENCES `Actor`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO `Actor` (id, firstName) VALUES (1, 'Alice')")
        db.execSQL(
            "INSERT INTO `Transaction` (id, type, actorId, channel, date, costEuro, description, tradeReferenceId) " +
                "VALUES (1, 'INBOUND', 1, 'Vinted', '2026-01-02', 12.5, 'first', NULL), " +
                "(2, 'OUTBOUND', 1, NULL, '2026-01-03', 3.0, 'second', 1)"
        )

        MIGRATION_19_20.migrate(db)

        db.query("SELECT id, actorId, channel, description, tradeReferenceId FROM `Transaction` ORDER BY id").use { c ->
            assertEquals(2, c.count)
            c.moveToFirst()
            assertEquals(1, c.getInt(1))
            assertEquals("Vinted", c.getString(2))
            assertEquals("first", c.getString(3))
            c.moveToNext()
            assertEquals(1, c.getInt(4))
        }

        db.execSQL(
            "INSERT INTO `Transaction` (type, actorId, date, costEuro, description) " +
                "VALUES ('INBOUND', NULL, '2026-01-04', 1.0, 'no actor')"
        )
        db.query("SELECT actorId FROM `Transaction` WHERE description = 'no actor'").use { c ->
            c.moveToFirst()
            assertTrue(c.isNull(0))
        }

        db.close()
    }
}
