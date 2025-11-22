package f.cking.software.data.database

import android.content.Context
import android.net.Uri
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import f.cking.software.data.database.dao.AppleContactDao
import f.cking.software.data.database.dao.DeviceDao
import f.cking.software.data.database.dao.JournalDao
import f.cking.software.data.database.dao.LocationDao
import f.cking.software.data.database.dao.RadarProfileDao
import f.cking.software.data.database.dao.TagDao
import f.cking.software.data.database.entity.AppleContactEntity
import f.cking.software.data.database.entity.DeviceEntity
import f.cking.software.data.database.entity.DeviceToLocationEntity
import f.cking.software.data.database.entity.JournalEntryEntity
import f.cking.software.data.database.entity.LocationEntity
import f.cking.software.data.database.entity.ProfileDetectEntity
import f.cking.software.data.database.entity.RadarProfileEntity
import f.cking.software.data.database.entity.TagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

@Database(
    entities = [
        DeviceEntity::class,
        RadarProfileEntity::class,
        AppleContactEntity::class,
        LocationEntity::class,
        DeviceToLocationEntity::class,
        JournalEntryEntity::class,
        TagEntity::class,
        ProfileDetectEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
    ],
    exportSchema = true,
    version = 19,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun radarProfileDao(): RadarProfileDao
    abstract fun appleContactDao(): AppleContactDao
    abstract fun locationDao(): LocationDao
    abstract fun journalDao(): JournalDao
    abstract fun tagDao(): TagDao

    suspend fun backupDatabase(toUri: Uri, context: Context) {
        Timber.i("Backup DB to file: ${toUri}")
        withContext(Dispatchers.IO) {
            val dbFile = File(context.getDatabasePath(openHelper.databaseName).toString())
            if (!dbFile.exists()) {
                throw IllegalStateException("The database file doesn't exist")
            }
            context.contentResolver.openOutputStream(toUri)?.use { outputStream ->
                dbFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw RuntimeException("Cannot create a backup file stream")
        }
    }

    suspend fun restoreDatabase(fromUri: Uri, context: Context) {
        withContext(Dispatchers.IO) {
            close()

            val contentResolver = context.contentResolver
            val tmpDatabaseName = openHelper.databaseName + "_tmp"
            val dbFile = File(context.getDatabasePath(openHelper.databaseName).toString())
            val tmpFile = File(context.getDatabasePath(tmpDatabaseName).toString())

            if (!tmpFile.exists()) {
                tmpFile.createNewFile()
            }

            contentResolver.openInputStream(fromUri).use { inputStream ->
                inputStream?.copyTo(tmpFile.outputStream()) ?: throw RuntimeException("Cannot open file")
            }

            try {
                testDatabase(tmpDatabaseName, context)
            } catch (e: Throwable) {
                tmpFile.delete()
                throw IllegalStateException("Cannot restore database from selected file")
            }

            tmpFile.renameTo(dbFile)
            tmpFile.delete()
        }
    }

    suspend fun getDatabaseSize(context: Context): Long {
        return withContext(Dispatchers.IO) {
            val dbFile = File(context.getDatabasePath(openHelper.databaseName).toString())
            dbFile.length()
        }
    }

    private fun testDatabase(name: String, context: Context) {
        val testDb = build(context, name)
        testDb.openHelper.writableDatabase.isDatabaseIntegrityOk
        testDb.close()
    }

    companion object {
        val loadDatabase = MutableStateFlow(false)

        fun build(context: Context, name: String): AppDatabase {
            loadDatabase.tryEmit(true)
            Timber.d("Build database: $name")
            val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_8_9,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                )
                .build()
            Timber.d("Database is ready!")
            loadDatabase.tryEmit(false)
            return database
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

        private val MIGRATION_12_13 = migration(12, 13) {
            it.execSQL("ALTER TABLE device ADD COLUMN system_address_type INTEGER DEFAULT NULL;")
            it.execSQL("ALTER TABLE device ADD COLUMN device_class INTEGER DEFAULT NULL;")
            it.execSQL("ALTER TABLE device ADD COLUMN is_paired INTEGER NOT NULL DEFAULT 0;")
        }

        private val MIGRATION_13_14 = migration(13, 14) {
            it.execSQL("ALTER TABLE device ADD COLUMN service_uuids TEXT NOT NULL DEFAULT '';")
        }

        private val MIGRATION_14_15 = migration(14, 15) {
            it.execSQL("ALTER TABLE device ADD COLUMN row_data_encoded TEXT DEFAULT NULL;")
        }

        val MIGRATION_15_16 = migration(15, 16) {
            it.execSQL("CREATE INDEX IF NOT EXISTS index_device_to_location ON device_to_location(device_address, location_time);")
            it.execSQL("CREATE INDEX IF NOT EXISTS index_location_time ON location(time);")
        }

        val MIGRATION_16_17 = migration(16, 17) {
            it.execSQL("ALTER TABLE device ADD COLUMN metadata TEXT DEFAULT NULL;")
        }

        val MIGRATION_17_18 = migration(17, 18) {
            it.execSQL("ALTER TABLE device ADD COLUMN is_connectable INTEGER NOT NULL DEFAULT 0;")
        }

        val MIGRATION_18_19 = migration(18, 19) {
            it.execSQL("ALTER TABLE radar_profile ADD COLUMN cooldown_ms INTEGER DEFAULT NULL;")
            it.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS profile_detect (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        profile_id INTEGER NOT NULL,
                        trigger_time INTEGER NOT NULL,
                        device_address TEXT NOT NULL
                    )
                """.trimIndent()
            )
            it.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_profile_detect_profile_id_trigger_time 
                ON profile_detect(profile_id, trigger_time)
            """.trimIndent()
            )
        }

        private fun migration(
            from: Int,
            to: Int,
            migrationFun: (database: SupportSQLiteDatabase) -> Unit
        ): Migration {
            return object : Migration(from, to) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    loadDatabase.value = true
                    migrationFun.invoke(database)
                    loadDatabase.value = false
                }
            }
        }
    }
}