package f.cking.software.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocationDao {

    @Query("SELECT location.time, location.lat, location.lng FROM device_to_location INNER JOIN location ON device_to_location.location_time = location.time WHERE device_address LIKE :address AND location.time >= :fromTime AND location.time <= :toTime")
    fun getAllLocationsByDeviceAddress(address: String, fromTime: Long = 0, toTime: Long = Long.MAX_VALUE): List<LocationEntity>

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