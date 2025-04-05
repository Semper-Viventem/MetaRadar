package f.cking.software.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "device_to_location", indices = [Index(value = ["device_address", "location_time"])])
data class DeviceToLocationEntity(
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Long? = null,
    @ColumnInfo(name = "device_address") val deviceAddress: String,
    @ColumnInfo(name = "location_time") val locationTime: Long,
)
