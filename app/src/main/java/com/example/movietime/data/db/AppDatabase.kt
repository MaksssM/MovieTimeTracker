package com.example.movietime.data.db
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WatchedItem::class, PlannedItem::class, WatchingItem::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun watchedItemDao(): WatchedItemDao
    abstract fun plannedItemDao(): PlannedDao
    abstract fun watchingItemDao(): WatchingDao


    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with composite primary key (id, mediaType)
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `watched_items_new` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `posterPath` TEXT, `releaseDate` TEXT, `runtime` INTEGER, `mediaType` TEXT NOT NULL, PRIMARY KEY(`id`,`mediaType`))"
                )

                // Copy existing data into the new table
                database.execSQL(
                    "INSERT INTO `watched_items_new` (id, title, posterPath, releaseDate, runtime, mediaType) SELECT id, title, posterPath, releaseDate, runtime, mediaType FROM `watched_items`"
                )

                // Remove old table
                database.execSQL("DROP TABLE IF EXISTS `watched_items`")

                // Rename new table to the original name
                database.execSQL("ALTER TABLE `watched_items_new` RENAME TO `watched_items`")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create planned items table with same structure
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `planned_items` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `posterPath` TEXT, `releaseDate` TEXT, `runtime` INTEGER, `mediaType` TEXT NOT NULL, PRIMARY KEY(`id`,`mediaType`))"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add dateAdded column to planned_items table
                database.execSQL("ALTER TABLE `planned_items` ADD COLUMN `dateAdded` INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create watching items table
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `watching_items` (
                        `id` INTEGER NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `posterPath` TEXT, 
                        `releaseDate` TEXT, 
                        `runtime` INTEGER, 
                        `mediaType` TEXT NOT NULL, 
                        `dateAdded` INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()},
                        `currentEpisode` INTEGER,
                        `currentSeason` INTEGER,
                        PRIMARY KEY(`id`,`mediaType`)
                    )"""
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add overview and voteAverage columns to watched_items table
                database.execSQL("ALTER TABLE `watched_items` ADD COLUMN `overview` TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE `watched_items` ADD COLUMN `voteAverage` REAL DEFAULT NULL")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add userRating column to watched_items table
                database.execSQL("ALTER TABLE `watched_items` ADD COLUMN `userRating` REAL DEFAULT NULL")
            }
        }
    }
}