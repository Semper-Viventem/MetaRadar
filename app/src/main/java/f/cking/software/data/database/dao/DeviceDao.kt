package f.cking.software.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import f.cking.software.data.database.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Query("SELECT * FROM device")
    fun getAll(): List<DeviceEntity>

    @Query("SELECT * FROM device")
    fun observeAll(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM device ORDER BY last_detect_time_ms DESC LIMIT :limit OFFSET :offset")
    fun getPaginated(offset: Int, limit: Int): List<DeviceEntity>

    @Query("SELECT * FROM device WHERE last_detect_time_ms >= :lastDetectTime ORDER BY last_detect_time_ms DESC")
    fun getByLastDetectTime(lastDetectTime: Long): List<DeviceEntity>

    @Query("SELECT * FROM device WHERE address LIKE :address")
    fun findByAddress(address: String): DeviceEntity?

    @Query("SELECT * FROM device WHERE address IN (:addresses)")
    fun findAllByAddresses(addresses: List<String>): List<DeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(deviceEntity: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(devices: List<DeviceEntity>)

    @Query("DELETE FROM device WHERE address LIKE :address")
    fun delete(address: String)

    @Query("DELETE FROM device WHERE address IN (:addresses)")
    fun deleteAllByAddress(addresses: List<String>)
}