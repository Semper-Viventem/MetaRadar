package f.cking.software.data.database

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
    @ColumnInfo(name = "custom_name") val customName: String? = null,
    @ColumnInfo(name = "favorite") val favorite: Boolean = false,
    @ColumnInfo(name = "manufacturer_id") val manufacturerId: Int? = null,
    @ColumnInfo(name = "manufacturer_name") val manufacturerName: String? = null,
)