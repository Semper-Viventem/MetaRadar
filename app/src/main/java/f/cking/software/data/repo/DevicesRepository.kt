package f.cking.software.data.repo

import f.cking.software.data.database.AppDatabase
import f.cking.software.data.database.DeviceDao
import f.cking.software.data.database.DeviceEntity
import f.cking.software.domain.model.AppleAirDrop
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
    private val appleContactsDao = appDatabase.appleContactDao()
    private val allDevices = MutableStateFlow(emptyList<DeviceData>())

    suspend fun getDevices(): List<DeviceData> {
        return withContext(Dispatchers.IO) {
            deviceDao.getAll().map { it.toDomainWithAirDrop() }
        }
    }

    suspend fun observeDevices(): StateFlow<List<DeviceData>> {
        return withContext(Dispatchers.IO) {
            if (allDevices.value.isEmpty()) {
                notifyListeners()
            }
            allDevices
        }
    }

    suspend fun saveScanBatch(devices: List<DeviceData>) {
        devices.forEach { device ->
            saveDevice(device)
            device.manufacturerInfo?.airdrop?.let { airdrop ->
                saveAppleAirDrop(airdrop, device.address, device.lastDetectTimeMs)
            }

        }
        notifyListeners()
    }

    suspend fun changeFavorite(device: DeviceData) {
        withContext(Dispatchers.IO) {
            val new = device.copy(favorite = !device.favorite)
            deviceDao.insert(new.toData())
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
            deviceDao.findAllByAddresses(addresses).map { it.toDomainWithAirDrop() }
        }
    }

    suspend fun getAirdropByKnownAddress(address: String): AppleAirDrop? {
        return withContext(Dispatchers.IO) {
            appleContactsDao.getByAddress(address)
                .map { it.toDomain() }
                .takeIf { it.isNotEmpty() }
                ?.let { AppleAirDrop(it) }
        }
    }

    suspend fun saveAppleAirDrop(appleAirDrop: AppleAirDrop, associatedAddress: String, lastUpdateTime: Long) {
        withContext(Dispatchers.IO) {
            appleAirDrop.contacts.forEach { appleContactsDao.insert(it.toData(associatedAddress, lastUpdateTime)) }
        }
    }

    private suspend fun notifyListeners() {
        val data = getDevices()
        allDevices.emit(data)
    }

    private suspend fun saveDevice(device: DeviceData) {
        val existing: DeviceData? = withContext(Dispatchers.IO) {
            deviceDao.findByAddress(device.address)?.toDomainWithAirDrop()
        }

        withContext(Dispatchers.IO) {
            if (existing != null) {
                val updated = existing.mergeWithNewDetected(device)
                deviceDao.insert(updated.toData())
            } else {
                deviceDao.insert(device.toData())
            }
        }
    }

    private suspend fun DeviceEntity.toDomainWithAirDrop(): DeviceData {
        return toDomain(getAirdropByKnownAddress(address))
    }
}