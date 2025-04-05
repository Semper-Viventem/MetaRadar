package f.cking.software.data.repo

import f.cking.software.data.database.AppDatabase
import f.cking.software.data.database.DatabaseUtils
import f.cking.software.data.database.dao.DeviceDao
import f.cking.software.data.database.entity.DeviceEntity
import f.cking.software.domain.model.AppleAirDrop
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.toData
import f.cking.software.domain.toDomain
import f.cking.software.splitToBatches
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DevicesRepository(
    appDatabase: AppDatabase,
) {
    private val deviceDao: DeviceDao = appDatabase.deviceDao()
    private val appleContactsDao = appDatabase.appleContactDao()
    private val lastBatch = MutableStateFlow(emptyList<DeviceData>())
    private val allDevices = MutableStateFlow(emptyList<DeviceData>())

    suspend fun getDevices(): List<DeviceData> =
        withContext(Dispatchers.IO) {
            deviceDao.getAll().toDomainWithAirDrop()
        }

    suspend fun getPaginated(
        offset: Int,
        limit: Int,
    ): List<DeviceData> =
        withContext(Dispatchers.IO) {
            deviceDao.getPaginated(offset, limit).toDomainWithAirDrop()
        }

    suspend fun getLastBatch(): List<DeviceData> =
        withContext(Dispatchers.IO) {
            val lastDevice = deviceDao.getPaginated(0, 1).firstOrNull()

            if (lastDevice == null) {
                emptyList()
            } else {
                val scanTime = lastDevice.lastDetectTimeMs
                deviceDao.getByLastDetectTime(scanTime).toDomainWithAirDrop()
            }
        }

    fun clearLastBatch() {
        lastBatch.value = emptyList()
    }

    suspend fun observeAllDevices(): StateFlow<List<DeviceData>> =
        allDevices.apply {
            if (allDevices.value.isEmpty()) {
                notifyListeners()
            }
        }

    suspend fun observeLastBatch(): StateFlow<List<DeviceData>> =
        lastBatch.apply {
            if (lastBatch.value.isEmpty()) {
                notifyListeners()
            }
        }

    suspend fun saveScanBatch(devices: List<DeviceData>) {
        withContext(Dispatchers.IO) {
            saveDevices(devices)
            saveContacts(devices)
            notifyListeners()
        }
    }

    suspend fun saveDevice(data: DeviceData) {
        withContext(Dispatchers.IO) {
            deviceDao.insert(data.toData())
            notifyListeners()
        }
    }

    suspend fun saveFollowingDetection(
        device: DeviceData,
        detectionTime: Long,
    ) {
        withContext(Dispatchers.IO) {
            val new = device.copy(lastFollowingDetectionTimeMs = detectionTime)
            deviceDao.insert(new.toData())
            notifyListeners()
        }
    }

    suspend fun deleteAllByAddress(addresses: List<String>) {
        withContext(Dispatchers.IO) {
            addresses.splitToBatches(DatabaseUtils.getMaxSQLVariablesNumber()).forEach { addressesBatch ->
                deviceDao.deleteAllByAddress(addressesBatch)
            }
            notifyListeners()
        }
    }

    suspend fun getAllByAddresses(addresses: List<String>): List<DeviceData> =
        withContext(Dispatchers.IO) {
            addresses.splitToBatches(DatabaseUtils.getMaxSQLVariablesNumber()).flatMap {
                deviceDao.findAllByAddresses(addresses).toDomainWithAirDrop()
            }
        }

    suspend fun getDeviceByAddress(address: String): DeviceData? =
        withContext(Dispatchers.IO) {
            deviceDao.findByAddress(address)?.toDomainWithAirDrop()
        }

    suspend fun getAirdropByKnownAddress(address: String): AppleAirDrop? =
        withContext(Dispatchers.IO) {
            appleContactsDao
                .getByAddress(address)
                .map { it.toDomain() }
                .takeIf { it.isNotEmpty() }
                ?.let { AppleAirDrop(it) }
        }

    suspend fun getAllBySHA(sha: List<Int>): List<AppleAirDrop.AppleContact> =
        withContext(Dispatchers.IO) {
            appleContactsDao.getBySHA(sha).map { it.toDomain() }
        }

    private suspend fun saveDevices(devices: List<DeviceData>) {
        withContext(Dispatchers.IO) {
            deviceDao.insertAll(devices.map { it.toData() })
        }
    }

    private suspend fun saveContacts(devices: List<DeviceData>) {
        withContext(Dispatchers.IO) {
            val contacts =
                devices.flatMap { device ->
                    device.manufacturerInfo
                        ?.airdrop
                        ?.contacts
                        ?.map { it.toData(device.address) } ?: emptyList()
                }

            appleContactsDao.insertAll(contacts)
        }
    }

    private suspend fun notifyListeners() {
        coroutineScope {
            launch {
                val data = getLastBatch()
                lastBatch.emit(data)
            }
            launch {
                val data = getDevices()
                allDevices.emit(data)
            }
        }
    }

    private suspend fun DeviceEntity.toDomainWithAirDrop(): DeviceData =
        withContext(Dispatchers.IO) {
            val contacts = appleContactsDao.getByAddress(address)
            toDomain(AppleAirDrop(contacts.map { it.toDomain() }))
        }

    private suspend fun List<DeviceEntity>.toDomainWithAirDrop(): List<DeviceData> =
        withContext(Dispatchers.IO) {
            val allRelatedContacts =
                splitToBatches(DatabaseUtils.getMaxSQLVariablesNumber()).flatMap { batch ->
                    appleContactsDao.getByAddresses(batch.map { it.address })
                }

            map { device ->
                val airdrop =
                    allRelatedContacts
                        .asSequence()
                        .filter { it.associatedAddress == device.address }
                        .map { it.toDomain() }
                        .toList()
                        .takeIf { it.isNotEmpty() }
                        ?.let { AppleAirDrop(it) }

                device.toDomain(airdrop)
            }
        }
}
