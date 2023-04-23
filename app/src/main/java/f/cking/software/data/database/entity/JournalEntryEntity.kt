package f.cking.software.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int?,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "reportJson") val report: String,
)