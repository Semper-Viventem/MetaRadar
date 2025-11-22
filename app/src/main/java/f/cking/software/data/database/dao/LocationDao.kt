package f.cking.software.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import f.cking.software.data.database.entity.DeviceToLocationEntity
import f.cking.software.data.database.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("""
        SELECT location.time, location.lat, location.lng 
        FROM location
        INNER JOIN (
            SELECT location_time FROM device_to_location 
            WHERE device_address = :address 
              AND location_time BETWEEN :fromTime AND :toTime
        ) AS dtl 
        ON dtl.location_time = location.time;
    """)
    fun getAllLocationsByDeviceAddress(address: String, fromTime: Long = 0, toTime: Long = Long.MAX_VALUE): List<LocationEntity>

    @Query("SELECT * FROM location")
    fun observeAllLocations(): Flow<List<LocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveLocation(locationEntity: LocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveLocationToDevice(deviceToLocationEntity: List<DeviceToLocationEntity>)

    @Query("DELETE FROM location")
    fun removeAllLocations()

    @Query("DELETE FROM device_to_location")
    fun removeAllDeviceToLocation()

    @Query("DELETE FROM device_to_location WHERE device_address IN (:addresses)")
    fun removeDeviceLocationsByAddresses(addresses: List<String>)
}