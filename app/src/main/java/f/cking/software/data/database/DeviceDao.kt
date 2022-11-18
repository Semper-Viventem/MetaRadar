package f.cking.software.data.database

import androidx.room.*

@Dao
interface DeviceDao {

    @Query("SELECT * FROM device")
    fun getAll(): List<DeviceEntity>

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