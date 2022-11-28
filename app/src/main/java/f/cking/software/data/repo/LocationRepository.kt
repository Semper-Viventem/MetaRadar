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

    suspend fun saveLocation(location: LocationModel, time: Long, detectedAddresses: List<String>) {
        withContext(Dispatchers.IO) {
            locationDao.saveLocation(location.toData(time))
            val addresses = detectedAddresses.map { DeviceToLocationEntity(deviceAddress = it, locationTime = time) }
            locationDao.saveLocationToDevice(addresses)
        }
    }

    suspend fun getAllAddressesForDevice(deviceAddress: String): List<LocationModel> {
        return withContext(Dispatchers.IO) {
            locationDao.getAllLocationsByDeviceAddress(deviceAddress).map { it.toDomain() }
        }
    }
}