package f.cking.software.data.database

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Database(
    entities = [
        DeviceEntity::class,
        RadarProfileEntity::class,
        AppleContactEntity::class,
        LocationEntity::class,
        DeviceToLocationEntity::class,
        JournalEntryEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 9, to = 10),
    ],
    exportSchema = true,
    version = 10,
)
abstract class AppDatabase : RoomDatabase() {

    private val TAG = "AppDatabase"

    lateinit var databaseName: String

    abstract fun deviceDao(): DeviceDao
    abstract fun radarProfileDao(): RadarProfileDao
    abstract fun appleContactDao(): AppleContactDao
    abstract fun locationDao(): LocationDao

    abstract fun journalDao(): JournalDao

    suspend fun backupDatabase(toUri: Uri, context: Context) {
        Log.i(TAG, "Backup DB to file: ${toUri}")
        withContext(Dispatchers.IO) {
            val dbFile = File(context.getDatabasePath(databaseName).toString())
            if (!dbFile.exists()) {
                throw IllegalStateException("The database file doesn't exist")
            }
            context.contentResolver.openFileDescriptor(toUri, "w")?.use { target ->
                FileOutputStream(target.fileDescriptor).use { outputStream ->
                    outputStream.write(dbFile.readBytes())
                }
            }
        }
    }

    suspend fun restoreDatabase(fromFile: File, context: Context) {
        withContext(Dispatchers.IO) {
            val dbFile = File(context.getDatabasePath(databaseName).toString())
            if (!dbFile.exists()) {
                dbFile.createNewFile()
            }
            if (!fromFile.exists()) {
                throw IllegalArgumentException("The backup file doesn't exist (${fromFile.path})")
            }
            fromFile.copyTo(dbFile, overwrite = true)
        }
    }

    companion object {
        fun build(context: Context, name: String): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_8_9,
                )
                .build()
                .apply {
                    databaseName = name
                }
        }

        private val MIGRATION_2_3 = migration(2, 3) {
            it.execSQL("ALTER TABLE device ADD COLUMN manufacturer_id INTEGER DEFAULT NULL;")
            it.execSQL("ALTER TABLE device ADD COLUMN manufacturer_name TEXT DEFAULT NULL;")
        }

        private val MIGRATION_3_4 = migration(3, 4) {
            it.execSQL(
                "CREATE TABLE `radar_profile` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`description` TEXT DEFAULT NULL, " +
                        "`is_active` INTEGER NOT NULL DEFAULT 1, " +
                        "`detect_filter` TEXT DEFAULT NULL, " +
                        "PRIMARY KEY(`id`));"
            )
        }

        private val MIGRATION_4_5 = migration(4, 5) {
            it.execSQL("DROP TABLE `radar_profile`;")
            it.execSQL(
                "CREATE TABLE `radar_profile` (" +
                        "`id` INTEGER, " +
                        "`name` TEXT NOT NULL, " +
                        "`description` TEXT DEFAULT NULL, " +
                        "`is_active` INTEGER NOT NULL DEFAULT 1, " +
                        "`detect_filter` TEXT DEFAULT NULL, " +
                        "PRIMARY KEY(`id`));"
            )
        }

        private val MIGRATION_5_6 = migration(5, 6) {
            it.execSQL(
                "CREATE TABLE `apple_contacts` (" +
                        "`sha_256` INTEGER NOT NULL, " +
                        "`associated_address` TEXT NOT NULL, " +
                        "`last_detect_time_ms` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`sha_256`));"
            )
        }

        private val MIGRATION_6_7 = migration(6, 7) {
            it.execSQL("DROP TABLE `apple_contacts`;")
            it.execSQL(
                "CREATE TABLE `apple_contacts` (" +
                        "`sha_256` INTEGER NOT NULL, " +
                        "`associated_address` TEXT NOT NULL, " +
                        "`first_detect_time_ms` INTEGER NOT NULL, " +
                        "`last_detect_time_ms` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`sha_256`));"
            )
        }

        private val MIGRATION_8_9 = migration(8, 9) {
            it.execSQL("ALTER TABLE device ADD COLUMN last_following_detection_ms INTEGER DEFAULT NULL;")
        }

        private fun migration(
            from: Int,
            to: Int,
            migrationFun: (database: SupportSQLiteDatabase) -> Unit
        ): Migration {
            return object : Migration(from, to) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    migrationFun.invoke(database)
                }
            }
        }
    }
}