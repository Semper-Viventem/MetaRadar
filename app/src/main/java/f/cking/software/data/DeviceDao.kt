package f.cking.software.data

import androidx.room.*

@Dao
interface DeviceDao {

    @Query("SELECT * FROM device")
    fun getAll(): List<DeviceEntity>

    @Query("SELECT * FROM device WHERE address LIKE :address")
    fun findByAddress(address: String): DeviceEntity?

    @Update
    fun update(deviceEntity: DeviceEntity)

    @Insert
    fun insert(deviceEntity: DeviceEntity)

    @Delete
    fun delete(deviceEntity: DeviceEntity)
}