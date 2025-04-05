package f.cking.software.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profile_detect", indices = [Index(value = ["profile_id", "trigger_time"])])
data class ProfileDetectEntity(
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Int?,
    @ColumnInfo(name = "profile_id") val profileId: Int,
    @ColumnInfo(name = "trigger_time") val triggerTime: Long,
    @ColumnInfo(name = "device_address") val deviceAddress: String,
)
