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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DevicesRepository(
    appDatabase: AppDatabase,
) {

    private val deviceDao: DeviceDao = appDatabase.deviceDao()
    private val allDevices = MutableStateFlow(emptyList<DeviceData>())

    init {
        notifyListeners()
    }

    fun getDevices() = deviceDao.getAll().map { it.toDomain() }
    fun observeDevices(): StateFlow<List<DeviceData>> = allDevices

    fun saveScanBatch(devices: List<BleScanDevice>) {
        devices.forEach { saveScanResult(it) }
        notifyListeners()
    }

    fun changeFavorite(device: DeviceData) {
        val new = device.copy(favorite = !device.favorite)
        deviceDao.update(new.toData())
        notifyListeners()
    }

    fun deleteDevice(device: DeviceData) {
        deviceDao.delete(device.toData())
        notifyListeners()
    }

    fun getAllByAddresses(addresses: List<String>): List<DeviceData> {
        return deviceDao.findAllByAddresses(addresses).map { it.toDomain() }
    }

    private fun notifyListeners() {
        runBlocking {
            launch(Dispatchers.IO) {
                val data = getDevices()
                allDevices.tryEmit(data)
            }
        }
    }

    private fun saveScanResult(device: BleScanDevice) {
        val existing = deviceDao.findByAddress(device.address)?.toDomain()

        if (existing != null) {
            updateExisting(existing, device)
        } else {
            createNew(device)
        }
    }

    private fun createNew(device: BleScanDevice) {
        val dataItem = DeviceData(
            address = device.address,
            name = device.name,
            lastDetectTimeMs = device.scanTimeMs,
            firstDetectTimeMs = device.scanTimeMs,
            detectCount = 1,
            customName = null,
            favorite = false,
        )

        deviceDao.insert(dataItem.toData())
    }

    private fun updateExisting(existing: DeviceData, device: BleScanDevice) {
        val newData = existing.copy(
            detectCount = existing.detectCount + 1,
            lastDetectTimeMs = device.scanTimeMs,
        )
        deviceDao.update(newData.toData())
    }
}