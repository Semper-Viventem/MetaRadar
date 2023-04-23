package f.cking.software.data.repo

import f.cking.software.data.database.AppDatabase
import f.cking.software.data.database.DatabaseUtils
import f.cking.software.data.database.entity.DeviceToLocationEntity
import f.cking.software.domain.model.LocationModel
import f.cking.software.domain.toData
import f.cking.software.domain.toDomain
import f.cking.software.splitToBatches
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationRepository(
    appDatabase: AppDatabase,
) {

    val locationDao = appDatabase.locationDao()

    suspend fun saveLocation(location: LocationModel, detectedAddresses: List<String>) {
        withContext(Dispatchers.IO) {
            locationDao.saveLocation(location.toData())
            val addresses = detectedAddresses.map {
                DeviceToLocationEntity(
                    deviceAddress = it,
                    locationTime = location.time
                )
            }
            locationDao.saveLocationToDevice(addresses)
        }
    }

    suspend fun getAllLocationsByAddress(
        deviceAddress: String,
        fromTime: Long = 0,
        toTime: Long = Long.MAX_VALUE,
    ): List<LocationModel> {
        return withContext(Dispatchers.IO) {
            locationDao.getAllLocationsByDeviceAddress(deviceAddress, fromTime, toTime)
                .map { it.toDomain() }
        }
    }

    suspend fun removeAllLocations() {
        withContext(Dispatchers.IO) {
            locationDao.removeAllLocations()
            locationDao.removeAllDeviceToLocation()
        }
    }

    suspend fun removeDeviceLocationsByAddresses(addresses: List<String>) {
        withContext(Dispatchers.IO) {
            addresses.splitToBatches(DatabaseUtils.getMaxSQLVariablesNumber()).forEach { addressesBatch ->
                locationDao.removeDeviceLocationsByAddresses(addresses)
            }
        }
    }
}