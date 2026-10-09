package ro.ddnostalgia.duelmastersinventory.shared.data

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=OFF")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `Actor` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `firstName` TEXT,
                `lastName` TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ActorAlias` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `actorId` INTEGER NOT NULL,
                `platform` TEXT NOT NULL,
                `username` TEXT NOT NULL,
                FOREIGN KEY(`actorId`) REFERENCES `Actor`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ActorAlias_actorId` ON `ActorAlias` (`actorId`)")

        db.execSQL(
            """
            CREATE TABLE `Transaction_new` (
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

        // Backfill: each distinct legacy `actor` free-text value becomes one Actor
        // (unnamed) with a single "legacy" alias carrying the original string, so
        // existing transactions keep showing something meaningful after the
        // display-resolution fallback (name -> channel-matched alias -> any alias).
        val insertActorStmt = db.compileStatement(
            "INSERT INTO `Actor` (firstName, lastName) VALUES (NULL, NULL)"
        )

        try {
            val cursor = db.query("SELECT DISTINCT actor FROM `Transaction`")
            cursor.use {
                while (it.moveToNext()) {
                    val legacyActor = it.getString(0)

                    val actorId = insertActorStmt.executeInsert()

                    db.execSQL(
                        "INSERT INTO `ActorAlias` (actorId, platform, username) VALUES (?, 'legacy', ?)",
                        arrayOf<Any?>(actorId, legacyActor)
                    )

                    db.execSQL(
                        """
                        INSERT INTO `Transaction_new`
                            (id, type, actorId, channel, date, costEuro, description, tradeReferenceId, parcelTrackingNumber)
                        SELECT id, type, ?, NULL, date, costEuro, description, tradeReferenceId, parcelTrackingNumber
                        FROM `Transaction` WHERE actor = ?
                        """.trimIndent(),
                        arrayOf<Any?>(actorId, legacyActor)
                    )
                }
            }
        } finally {
            insertActorStmt.close()
        }

        db.execSQL("DROP TABLE `Transaction`")
        db.execSQL("ALTER TABLE `Transaction_new` RENAME TO `Transaction`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_Transaction_tradeReferenceId` ON `Transaction` (`tradeReferenceId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_Transaction_actorId` ON `Transaction` (`actorId`)")

        val fkCheck = db.query("PRAGMA foreign_key_check")
        fkCheck.use {
            if (it.count > 0) {
                Log.e("MIGRATION_6_7", "foreign_key_check found ${it.count} violation(s) after migration")
                check(false) { "MIGRATION_6_7 left ${it.count} foreign key violation(s) in the database" }
            }
        }
    }
}

// The MIGRATION_6_7 backfill carried the pre-migration free-text `actor` value
// (often itself shaped like "{platform}/{username}", e.g. "ebay/xyz") into a
// single `platform='legacy'` ActorAlias.username verbatim. Split those back
// into a proper platform + username pair wherever a '/' is present, so they
// display and channel-match the same way as regular aliases.
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE `ActorAlias`
            SET
                platform = substr(username, 1, instr(username, '/') - 1),
                username = substr(username, instr(username, '/') + 1)
            WHERE platform = 'legacy' AND instr(username, '/') > 0
            """.trimIndent()
        )
    }
}

// Adds CardPrototypeEmbedding, storing a precomputed semantic-search vector per
// CardPrototype. Rows are backfilled lazily by DatabaseModule's onOpen callback
// (guarded by a count()==0 check), not by this migration, since populating them
// requires running the on-device embedding model, not a SQL transform.
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `CardPrototypeEmbedding` (
                `cardPrototypeId` INTEGER NOT NULL PRIMARY KEY,
                `vector` BLOB NOT NULL,
                FOREIGN KEY(`cardPrototypeId`) REFERENCES `CardPrototype`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }
}

// The embedded text used to seed CardPrototypeEmbedding changed from
// "name + races + text" to "text only" (name/races were polluting the
// semantic signal - e.g. a card named "Turnip" spuriously matching a query
// containing "turn" on subword overlap alone). Existing rows were computed
// against the old definition, so they're stale; clear them out and let
// DatabaseModule's onOpen (guarded by count()==0) recompute them against the
// new one on next launch.
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM `CardPrototypeEmbedding`")
    }
}

// Same reasoning as MIGRATION_9_10: the embedded text changed again, this
// time to strip parenthetical reminder text (e.g. "Shield trigger (When this
// spell is put into your hand from your shield zone...)"), which is
// near-identical boilerplate across a huge share of the card pool and was
// making unrelated shield-trigger cards cluster together for any shield/hand
// query. Clear and let onOpen recompute.
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM `CardPrototypeEmbedding`")
    }
}

// Same reasoning again: vanilla/textless cards used to fall back to embedding
// their own name, which reintroduced name-collision noise (an OOV proper
// noun's embedding is mostly meaningless subword soup that clusters
// spuriously with other proper-noun-only embeddings - e.g. a plain name
// query like "Bajula" matched dozens of unrelated cards at an oddly uniform
// ~0.5 similarity). Those cards are now skipped entirely (no embedding row).
// Clear and let onOpen recompute.
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM `CardPrototypeEmbedding`")
    }
}

// Adds the perceptual-hash index table backing camera scan matching
// (DatabaseModule builds it lazily on next open, same as the embedding index).
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `CardImageHash` (
                `cardPrintId` INTEGER NOT NULL,
                `hash` INTEGER NOT NULL,
                PRIMARY KEY(`cardPrintId`)
            )
            """.trimIndent()
        )
    }
}

// Swaps the perceptual-hash scan index for ORB local-feature descriptors
// (see decisions/0009-orb-feature-matching.md) - CardImageHash's single
// 64-bit column can't hold a variable-length descriptor set, so this is a
// drop+recreate under a new name rather than an ALTER. Purely a rebuildable
// cache table (assets/card_images/*.jpg -> index), same reasoning as
// MIGRATION_12_13; DatabaseModule's onOpen callback rebuilds it lazily on
// next launch.
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `CardImageHash`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `CardImageFeatures` (
                `cardPrintId` INTEGER NOT NULL,
                `descriptors` BLOB NOT NULL,
                `keypointCount` INTEGER NOT NULL,
                PRIMARY KEY(`cardPrintId`)
            )
            """.trimIndent()
        )
    }
}

// Adds the `keypoints` column to CardImageFeatures - matching now does RANSAC
// homography verification (decisions/0009 update) on top of descriptor
// distance, which needs each keypoint's (x, y) location, not just its
// descriptor bytes. Same reasoning as MIGRATION_13_14: purely a rebuildable
// cache table, so this is a drop+recreate; DatabaseModule's onOpen callback
// rebuilds it lazily on next launch.
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `CardImageFeatures`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `CardImageFeatures` (
                `cardPrintId` INTEGER NOT NULL,
                `descriptors` BLOB NOT NULL,
                `keypoints` BLOB NOT NULL,
                `keypointCount` INTEGER NOT NULL,
                PRIMARY KEY(`cardPrintId`)
            )
            """.trimIndent()
        )
    }
}

// Adds CardSetMeta - the display name for a (language, set) pair learned at
// import time from a set pack's data.json, for sets not in CardSetOrder's
// static bundled-set table (see "dynamic sets" spec).
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `CardSetMeta` (
                `language` TEXT NOT NULL,
                `set` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                PRIMARY KEY(`language`, `set`)
            )
            """.trimIndent()
        )
    }
}

// Adds the ability-keyword registry + its many-to-many link to CardPrototype, seeded with
// the five keywords catalogued in sources/set-packages/ISSUES.md at KB-writing time - see
// specs/0007-prototype-identity-normalization-ability-keywords.md. Only prototypes created
// from here on get parsed against it (CardSetImportRepository.matchOrCreatePrototype);
// existing rows are left as plain text, no backfill (spec's explicit out-of-scope call).
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `AbilityKeyword` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `matchText` TEXT NOT NULL,
                `iconKey` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `CardPrototypeAbilityKeyword` (
                `cardPrototypeId` INTEGER NOT NULL,
                `abilityKeywordId` INTEGER NOT NULL,
                PRIMARY KEY(`cardPrototypeId`, `abilityKeywordId`),
                FOREIGN KEY(`cardPrototypeId`) REFERENCES `CardPrototype`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`abilityKeywordId`) REFERENCES `AbilityKeyword`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_CardPrototypeAbilityKeyword_abilityKeywordId` ON `CardPrototypeAbilityKeyword` (`abilityKeywordId`)")

        val seed = listOf(
            "Blocker" to "blocker",
            "Shield Trigger" to "shield_trigger",
            "Slayer" to "slayer",
            "Civil Count" to "civil_count",
            "Guard Strike" to "guard_strike",
        )
        val insertStmt = db.compileStatement("INSERT INTO `AbilityKeyword` (matchText, iconKey) VALUES (?, ?)")
        try {
            for ((matchText, iconKey) in seed) {
                insertStmt.bindString(1, matchText)
                insertStmt.bindString(2, iconKey)
                insertStmt.executeInsert()
                insertStmt.clearBindings()
            }
        } finally {
            insertStmt.close()
        }
    }
}

// Backfills CardSetMeta for the 15 originally-hardcoded (language, set) pairs
// (decisions/0013 removed the bundled/dynamic distinction, but any install
// whose catalog predates CardSetMeta - MIGRATION_15_16 - never got a row for
// these, since the old CSV-seeding path never touched that table). Without
// this, the Sets screen's "only dynamically-imported sets are deletable"
// check (CardSetSummary.isDynamic, gated on "has a CardSetMeta row") wrongly
// leaves these 15 permanently non-deletable even though nothing is bundled
// anymore. Names match CardSetOrder.DISPLAY_NAMES; INSERT OR IGNORE so a set
// already re-imported dynamically (and thus already correctly named/present)
// is left untouched. Harmless no-op for a device that never had a given set.
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val seed = listOf(
            Triple("EN", "DM-01", "Base Set"),
            Triple("EN", "DM-02", "Evo-Crushinators of Doom"),
            Triple("EN", "DM-03", "Rampage of the Super Warriors"),
            Triple("EN", "DM-04", "Shadowclash of Blinding Night"),
            Triple("EN", "DM-05", "Survivors of the Megapocalypse"),
            Triple("EN", "DM-06", "Stomp-A-Trons of Invincible Wrath"),
            Triple("EN", "DM-07", "Thundercharge of Ultra Destruction"),
            Triple("EN", "DM-08", "Epic Dragons of Hyperchaos"),
            Triple("EN", "DM-09", "Fatal Brood of Infinite Ruin"),
            Triple("EN", "DM-10", "Shockwaves of the Shattered Rainbow"),
            Triple("EN", "DM-11", "Blast-o-Splosion of Gigantic Rage"),
            Triple("EN", "DM-12", "Thrash of the Hybrid Megacreatures"),
            Triple("EN", "CTD", "Collector's Tin Deck"),
            Triple("EN", "Promo", "Promo"),
            Triple("JP", "DM-01", "Basic Set"),
        )
        val insertStmt = db.compileStatement(
            "INSERT OR IGNORE INTO `CardSetMeta` (language, [set], name) VALUES (?, ?, ?)"
        )
        try {
            for ((language, set, name) in seed) {
                insertStmt.bindString(1, language)
                insertStmt.bindString(2, set)
                insertStmt.bindString(3, name)
                insertStmt.executeInsert()
                insertStmt.clearBindings()
            }
        } finally {
            insertStmt.close()
        }
    }
}

// Adds the optional `url` column to ActorAlias (e.g. a link to the actor's Vinted/eBay
// profile), surfaced on ActorViewScreen as a tap-to-open affordance on the alias row.
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `ActorAlias` ADD COLUMN `url` TEXT")
    }
}
