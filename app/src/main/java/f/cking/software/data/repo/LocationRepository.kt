package f.cking.software.data.repo

import f.cking.software.data.database.AppDatabase
import f.cking.software.data.database.DeviceToLocationEntity
import f.cking.software.domain.model.LocationModel
import f.cking.software.domain.toData
import f.cking.software.domain.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationRepository(
    private val appDatabase: AppDatabase,
) {

    val locationDao = appDatabase.locationDao()

    suspend fun saveLocation(location: LocationModel, detectedAddresses: List<String>) {
        withContext(Dispatchers.IO) {
            locationDao.saveLocation(location.toData())
            val addresses = detectedAddresses.map { DeviceToLocationEntity(deviceAddress = it, locationTime = location.time) }
            locationDao.saveLocationToDevice(addresses)
        }
    }

    suspend fun getAllLocationsByAddress(deviceAddress: String): List<LocationModel> {
        return withContext(Dispatchers.IO) {
            locationDao.getAllLocationsByDeviceAddress(deviceAddress).map { it.toDomain() }
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
            locationDao.removeDeviceLocationsByAddresses(addresses)
        }
    }
}