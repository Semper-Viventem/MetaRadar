package f.cking.software.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DeviceEntity::class],
    version = 2,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
}