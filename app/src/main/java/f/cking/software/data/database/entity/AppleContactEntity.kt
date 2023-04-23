package f.cking.software.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apple_contacts")
data class AppleContactEntity(
    @PrimaryKey @ColumnInfo(name = "sha_256") val sha256: Int,
    @ColumnInfo(name = "associated_address") val associatedAddress: String,
    @ColumnInfo(name = "first_detect_time_ms") val firstDetectTimeMs: Long,
    @ColumnInfo(name = "last_detect_time_ms") val lastDetectTimeMs: Long,
)