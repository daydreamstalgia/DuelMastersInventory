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
 * Exercises MIGRATION_7_8 directly: `legacy` aliases whose username is shaped
 * like "{platform}/{username}" (the raw pre-Actor free-text value) get split
 * into a real platform + username pair; anything else is left untouched.
 */
@RunWith(AndroidJUnit4::class)
class Migration7To8Test {
    private val dbName = "migration-7-8-test.db"
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
    fun migration_splitsLegacyPlatformUsernameAliases() {
        val db = openDatabase()

        db.execSQL(
            """
            CREATE TABLE `Actor` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `firstName` TEXT,
                `lastName` TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE `ActorAlias` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `actorId` INTEGER NOT NULL,
                `platform` TEXT NOT NULL,
                `username` TEXT NOT NULL,
                FOREIGN KEY(`actorId`) REFERENCES `Actor`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL("INSERT INTO `Actor` (id, firstName, lastName) VALUES (1, NULL, NULL)")
        db.execSQL("INSERT INTO `Actor` (id, firstName, lastName) VALUES (2, NULL, NULL)")

        // Splittable: legacy value shaped like "{platform}/{username}".
        db.execSQL(
            "INSERT INTO `ActorAlias` (id, actorId, platform, username) VALUES (1, 1, 'legacy', 'ebay/xyz')"
        )
        // Not splittable: no '/' in the legacy value, left as-is.
        db.execSQL(
            "INSERT INTO `ActorAlias` (id, actorId, platform, username) VALUES (2, 2, 'legacy', 'John Doe')"
        )
        // Already a real alias, untouched regardless of content.
        db.execSQL(
            "INSERT INTO `ActorAlias` (id, actorId, platform, username) VALUES (3, 1, 'vinted', 'legacy/thing')"
        )

        MIGRATION_7_8.migrate(db)

        db.query("SELECT platform, username FROM `ActorAlias` WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals("ebay", c.getString(0))
            assertEquals("xyz", c.getString(1))
        }
        db.query("SELECT platform, username FROM `ActorAlias` WHERE id = 2").use { c ->
            c.moveToFirst()
            assertEquals("legacy", c.getString(0))
            assertEquals("John Doe", c.getString(1))
        }
        db.query("SELECT platform, username FROM `ActorAlias` WHERE id = 3").use { c ->
            c.moveToFirst()
            assertEquals("vinted", c.getString(0))
            assertEquals("legacy/thing", c.getString(1))
        }

        db.close()
    }
}
