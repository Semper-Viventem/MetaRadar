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

    suspend fun getDevices(withAirdropInfo: Boolean): List<DeviceData> {
        return withContext(Dispatchers.IO) {
            deviceDao.getAll().map {
                if (withAirdropInfo) {
                    it.toDomainWithAirDrop()
                } else {
                    it.toDomain(null)
                }
            }
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
        withContext(Dispatchers.IO) {
            val airdrops = devices.flatMap { device ->
                device.manufacturerInfo?.airdrop?.contacts
                    ?.map { it.toData(device.address, device.lastDetectTimeMs) } ?: emptyList()
            }

            val mappedDevices = devices.map { mergeWithExisting(it).toData() }

            deviceDao.insertAll(mappedDevices)
            appleContactsDao.insertAll(airdrops)

            notifyListeners()
        }
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

    private suspend fun notifyListeners() {
        val data = getDevices(true)
        allDevices.emit(data)
    }

    private fun mergeWithExisting(device: DeviceData): DeviceData {
        val existing: DeviceData? = deviceDao.findByAddress(device.address)?.toDomain(appleAirDrop = null)
        return existing?.mergeWithNewDetected(device) ?: device
    }

    private suspend fun DeviceEntity.toDomainWithAirDrop(): DeviceData {
        return toDomain(getAirdropByKnownAddress(address))
    }
}