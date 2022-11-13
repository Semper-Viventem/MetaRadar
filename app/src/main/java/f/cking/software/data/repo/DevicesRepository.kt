package f.cking.software.data.repo

import f.cking.software.data.database.AppDatabase
import f.cking.software.data.database.DeviceDao
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.toData
import f.cking.software.domain.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class DevicesRepository(
    appDatabase: AppDatabase,
) {

    private val deviceDao: DeviceDao = appDatabase.deviceDao()
    private val allDevices = MutableStateFlow(emptyList<DeviceData>())

    suspend fun getDevices(): List<DeviceData> {
        return withContext(Dispatchers.IO) {
            deviceDao.getAll().map { it.toDomain() }
        }
    }

    suspend fun observeDevices(): StateFlow<List<DeviceData>> {
        return withContext(Dispatchers.IO) {
            if (allDevices.value.isEmpty()) {
                allDevices.emit(getDevices())
            }
            allDevices
        }
    }

    suspend fun saveScanBatch(devices: List<DeviceData>) {
        devices.forEach { saveScanResult(it) }
        notifyListeners()
    }

    suspend fun changeFavorite(device: DeviceData) {
        withContext(Dispatchers.IO) {
            val new = device.copy(favorite = !device.favorite)
            deviceDao.update(new.toData())
            notifyListeners()
        }
    }

    suspend fun deleteAllByAddress(addresses: List<String>) {
        withContext(Dispatchers.IO) {
            deviceDao.deleteAllByAddress(addresses)
            notifyListeners()
        }
    }

    suspend fun getAllByAddresses(addresses: List<String>): List<DeviceData> {
        return withContext(Dispatchers.IO) {
            deviceDao.findAllByAddresses(addresses).map { it.toDomain() }
        }
    }

    private suspend fun notifyListeners() {
        val data = getDevices()
        allDevices.emit(data)
    }

    private suspend fun saveScanResult(device: DeviceData) {
        val existing = withContext(Dispatchers.IO) {
            deviceDao.findByAddress(device.address)?.toDomain()
        }

        if (existing != null) {
            updateExisting(existing, device)
        } else {
            createNew(device)
        }
    }

    private suspend fun createNew(device: DeviceData) {
        withContext(Dispatchers.IO) {
            deviceDao.insert(device.toData())
        }
    }

    private suspend fun updateExisting(existing: DeviceData, new: DeviceData) {
        val updated = existing.mergeWithNewDetected(new)

        withContext(Dispatchers.IO) {
            deviceDao.update(updated.toData())
        }
    }
}