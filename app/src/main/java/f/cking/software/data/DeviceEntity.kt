package f.cking.software.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device")
data class DeviceEntity(
    @PrimaryKey @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "last_detect_time_ms") val lastDetectTimeMs: Long,
    @ColumnInfo(name = "first_detect_time_ms") val firstDetectTimeMs: Long,
    @ColumnInfo(name = "detect_count") val detectCount: Int,
)