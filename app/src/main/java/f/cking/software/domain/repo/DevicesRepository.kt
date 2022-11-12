package f.cking.software.domain.repo

import f.cking.software.data.AppDatabase
import f.cking.software.data.DeviceDao
import f.cking.software.domain.model.BleScanDevice
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

    suspend fun saveScanBatch(devices: List<BleScanDevice>) {
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

    private suspend fun saveScanResult(device: BleScanDevice) {
        val existing = withContext(Dispatchers.IO) {
            deviceDao.findByAddress(device.address)?.toDomain()
        }

        if (existing != null) {
            updateExisting(existing, device)
        } else {
            createNew(device)
        }
    }

    private suspend fun createNew(device: BleScanDevice) {
        val dataItem = DeviceData(
            address = device.address,
            name = device.name,
            lastDetectTimeMs = device.scanTimeMs,
            firstDetectTimeMs = device.scanTimeMs,
            detectCount = 1,
            customName = null,
            favorite = false,
        )

        withContext(Dispatchers.IO) {
            deviceDao.insert(dataItem.toData())
        }
    }

    private suspend fun updateExisting(existing: DeviceData, device: BleScanDevice) {
        val newData = existing.copy(
            detectCount = existing.detectCount + 1,
            lastDetectTimeMs = device.scanTimeMs,
        )

        withContext(Dispatchers.IO) {
            deviceDao.update(newData.toData())
        }
    }
}