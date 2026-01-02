package com.example.movietime.data.db
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WatchedItem::class, 
        PlannedItem::class, 
        WatchingItem::class, 
        SearchHistoryItem::class, 
        TvShowProgress::class,
        UserCollection::class,
        CollectionItem::class,
        RewatchEntry::class,
        FollowedPerson::class,
        YearlyStats::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun watchedItemDao(): WatchedItemDao
    abstract fun plannedItemDao(): PlannedDao
    abstract fun watchingItemDao(): WatchingDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun tvShowProgressDao(): TvShowProgressDao
    
    // New DAOs for extended features
    abstract fun userCollectionDao(): UserCollectionDao
    abstract fun collectionItemDao(): CollectionItemDao
    abstract fun rewatchDao(): RewatchDao
    abstract fun followedPersonDao(): FollowedPersonDao
    abstract fun yearlyStatsDao(): YearlyStatsDao


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

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add TV show specific columns to watched_items table
                database.execSQL("ALTER TABLE `watched_items` ADD COLUMN `episodeRuntime` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE `watched_items` ADD COLUMN `totalEpisodes` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE `watched_items` ADD COLUMN `isOngoing` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `watched_items` ADD COLUMN `status` TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE `watched_items` ADD COLUMN `lastUpdated` INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create search history table
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `search_history` (
                        `id` INTEGER NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `posterPath` TEXT, 
                        `mediaType` TEXT NOT NULL, 
                        `releaseDate` TEXT,
                        `voteAverage` REAL,
                        `timestamp` INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()},
                        PRIMARY KEY(`id`,`mediaType`)
                    )"""
                )
            }
        }
        
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create TV show progress table for tracking individual episodes
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `tv_show_progress` (
                        `tvShowId` INTEGER NOT NULL, 
                        `seasonNumber` INTEGER NOT NULL, 
                        `episodeNumber` INTEGER NOT NULL, 
                        `episodeName` TEXT,
                        `episodeRuntime` INTEGER,
                        `watched` INTEGER NOT NULL DEFAULT 0,
                        `watchedAt` INTEGER,
                        PRIMARY KEY(`tvShowId`, `seasonNumber`, `episodeNumber`)
                    )"""
                )
            }
        }
        
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // User Collections table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_collections` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `coverImagePath` TEXT,
                        `emoji` TEXT,
                        `color` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """)
                
                // Collection Items junction table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `collection_items` (
                        `collectionId` INTEGER NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `mediaType` TEXT NOT NULL,
                        `title` TEXT,
                        `posterPath` TEXT,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`collectionId`, `itemId`, `mediaType`),
                        FOREIGN KEY(`collectionId`) REFERENCES `user_collections`(`id`) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_items_collectionId` ON `collection_items` (`collectionId`)")
                
                // Rewatch Entries table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `rewatch_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `mediaType` TEXT NOT NULL,
                        `title` TEXT,
                        `posterPath` TEXT,
                        `watchedAt` INTEGER NOT NULL,
                        `userRating` REAL,
                        `notes` TEXT,
                        `watchTimeMinutes` INTEGER
                    )
                """)
                
                // Followed People table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `followed_people` (
                        `personId` INTEGER PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `profilePath` TEXT,
                        `knownForDepartment` TEXT,
                        `followedAt` INTEGER NOT NULL,
                        `notificationsEnabled` INTEGER NOT NULL DEFAULT 1
                    )
                """)
                
                // Yearly Stats table for Year in Review
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `yearly_stats` (
                        `year` INTEGER PRIMARY KEY NOT NULL,
                        `totalMovies` INTEGER NOT NULL DEFAULT 0,
                        `totalTvEpisodes` INTEGER NOT NULL DEFAULT 0,
                        `totalWatchTimeMinutes` INTEGER NOT NULL DEFAULT 0,
                        `favoriteGenreId` INTEGER,
                        `favoriteGenreName` TEXT,
                        `favoriteActorId` INTEGER,
                        `favoriteActorName` TEXT,
                        `favoriteDirectorId` INTEGER,
                        `favoriteDirectorName` TEXT,
                        `topRatedItemId` INTEGER,
                        `topRatedItemTitle` TEXT,
                        `topRatedItemRating` REAL,
                        `mostRewatchedItemId` INTEGER,
                        `mostRewatchedItemTitle` TEXT,
                        `mostRewatchedCount` INTEGER NOT NULL DEFAULT 0,
                        `longestMovieId` INTEGER,
                        `longestMovieTitle` TEXT,
                        `longestMovieRuntime` INTEGER,
                        `bingeWatchedSeriesId` INTEGER,
                        `bingeWatchedSeriesTitle` TEXT,
                        `uniqueGenresCount` INTEGER NOT NULL DEFAULT 0,
                        `uniqueActorsCount` INTEGER NOT NULL DEFAULT 0,
                        `uniqueDirectorsCount` INTEGER NOT NULL DEFAULT 0,
                        `monthlyBreakdown` TEXT,
                        `calculatedAt` INTEGER NOT NULL
                    )
                """)
            }
        }
        
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add genreIds column to watched_items table
                database.execSQL("ALTER TABLE `watched_items` ADD COLUMN `genreIds` TEXT DEFAULT NULL")
            }
        }
        
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add watchCount column to watched_items table if not exists
                database.execSQL("ALTER TABLE `watched_items` ADD COLUMN `watchCount` INTEGER NOT NULL DEFAULT 1")
                // Add watchDate column to track last watch date
                database.execSQL("ALTER TABLE `watched_items` ADD COLUMN `watchDate` INTEGER DEFAULT NULL")
            }
        }
    }
}