package ro.daydreamstalgia.duelmastersinventory.shared.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises MIGRATION_8_9: it should just create the CardPrototypeEmbedding
 * table (row backfill happens separately, lazily, in DatabaseModule's onOpen
 * callback - not part of the migration itself).
 */
@RunWith(AndroidJUnit4::class)
class Migration8To9Test {
    private val dbName = "migration-8-9-test.db"
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
    fun migration_createsEmbeddingTable() {
        val db = openDatabase()

        db.execSQL(
            """
            CREATE TABLE `CardPrototype` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `name` TEXT,
                `civilization` TEXT,
                `races` TEXT,
                `mana` INTEGER,
                `power` TEXT,
                `type` TEXT,
                `text` TEXT
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO `CardPrototype` (id, name) VALUES (1, 'Test Card')")

        MIGRATION_8_9.migrate(db)

        db.execSQL(
            "INSERT INTO `CardPrototypeEmbedding` (cardPrototypeId, vector) VALUES (1, ?)",
            arrayOf<Any?>(ByteArray(384 * 4))
        )

        db.query("SELECT COUNT(*) FROM `CardPrototypeEmbedding`").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }

        db.close()
    }
}
