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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DevicesRepository(
    appDatabase: AppDatabase,
) {

    private val deviceDao: DeviceDao = appDatabase.deviceDao()
    private val appleContactsDao = appDatabase.appleContactDao()
    private val lastBatch = MutableStateFlow(emptyList<DeviceData>())
    private val allDevices = deviceDao.observeAll()
        .map { it.toDomain(withAirdropInfo = true) }

    suspend fun getDevices(withAirdropInfo: Boolean = false): List<DeviceData> {
        return withContext(Dispatchers.IO) {
            deviceDao.getAll().toDomain(withAirdropInfo)
        }
    }

    suspend fun getPaginated(offset: Int, limit: Int): List<DeviceData> {
        return withContext(Dispatchers.IO) {
            deviceDao.getPaginated(offset, limit).toDomain(withAirdropInfo = true)
        }
    }

    suspend fun getLastBatch(): List<DeviceData> {
        return withContext(Dispatchers.IO) {
            val lastDevice = deviceDao.getPaginated(0, 1).firstOrNull()

            if (lastDevice == null) {
                emptyList()
            } else {
                val scanTime = lastDevice.lastDetectTimeMs
                deviceDao.getByLastDetectTime(scanTime).toDomain(withAirdropInfo = true)
            }
        }
    }

    fun clearLastBatch() {
        lastBatch.value = emptyList()
    }

    fun observeAllDevices(): Flow<List<DeviceData>> {
        return allDevices
    }

    suspend fun observeLastBatch(): StateFlow<List<DeviceData>> {
        return lastBatch.apply {
            if (lastBatch.value.isEmpty()) {
                notifyLastBatchListener()
            }
        }
    }

    suspend fun saveScanBatch(devices: List<DeviceData>) {
        withContext(Dispatchers.IO) {
            saveDevices(devices)
            saveContacts(devices)
            notifyLastBatchListener()
        }
    }

    suspend fun saveDevice(data: DeviceData) {
        withContext(Dispatchers.IO) {
            deviceDao.insert(data.toData())
            notifyLastBatchListener()
        }
    }

    suspend fun saveFollowingDetection(device: DeviceData, detectionTime: Long) {
        withContext(Dispatchers.IO) {
            val new = device.copy(lastFollowingDetectionTimeMs = detectionTime)
            deviceDao.insert(new.toData())
            notifyLastBatchListener()
        }
    }

    suspend fun deleteAllByAddress(addresses: List<String>) {
        withContext(Dispatchers.IO) {
            addresses.splitToBatches(DatabaseUtils.getMaxSQLVariablesNumber()).forEach { addressesBatch ->
                deviceDao.deleteAllByAddress(addressesBatch)
            }
            notifyLastBatchListener()
        }
    }

    suspend fun clearUnAssociatedAirdrops() {
        withContext(Dispatchers.IO) {
            val allDevices = deviceDao.getAll().mapTo(mutableSetOf()) { it.address }
            val allAidrops = appleContactsDao.getAll().map { it.associatedAddress }

            val unassotiatedAirdrops = allAidrops.filter { !allDevices.contains(it) }

            appleContactsDao.deleteAllByAddresses(unassotiatedAirdrops)
        }
    }

    suspend fun getAllByAddresses(addresses: List<String>): List<DeviceData> {
        return withContext(Dispatchers.IO) {
            addresses.splitToBatches(DatabaseUtils.getMaxSQLVariablesNumber()).flatMap {
                deviceDao.findAllByAddresses(addresses).toDomain(withAirdropInfo = true)
            }
        }
    }

    suspend fun getDeviceByAddress(address: String): DeviceData? {
        return withContext(Dispatchers.IO) {
            deviceDao.findByAddress(address)?.toDomainWithAirDrop()
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

    suspend fun getAllBySHA(sha: List<Int>): List<AppleAirDrop.AppleContact> {
        return withContext(Dispatchers.IO) {
            appleContactsDao.getBySHA(sha).map { it.toDomain() }
        }
    }

    private suspend fun saveDevices(devices: List<DeviceData>) {
        withContext(Dispatchers.IO) {
            deviceDao.insertAll(devices.map { it.toData() })
        }
    }

    private suspend fun saveContacts(devices: List<DeviceData>) {
        withContext(Dispatchers.IO) {
            val contacts = devices.flatMap { device ->
                device.manufacturerInfo?.airdrop?.contacts?.map { it.toData(device.address) } ?: emptyList()
            }

            appleContactsDao.insertAll(contacts)
        }
    }

    private suspend fun notifyLastBatchListener() {
        coroutineScope {
            launch(Dispatchers.Default) {
                val data = getLastBatch()
                lastBatch.emit(data)
            }
        }
    }

    private suspend fun DeviceEntity.toDomainWithAirDrop(): DeviceData {
        return withContext(Dispatchers.IO) {
            val contacts = appleContactsDao.getByAddress(address)
            toDomain(AppleAirDrop(contacts.map { it.toDomain() }))
        }
    }

    private suspend fun List<DeviceEntity>.toDomain(withAirdropInfo: Boolean): List<DeviceData> {
        return withContext(Dispatchers.Default) {
            if (withAirdropInfo) {
                toDomainWithAirDrop()
            } else {
                map { it.toDomain() }
            }
        }
    }

    private suspend fun List<DeviceEntity>.toDomainWithAirDrop(): List<DeviceData> {
        return withContext(Dispatchers.Default) {
            val allRelatedContacts = withContext(Dispatchers.IO) {
                appleContactsDao.getAll().groupBy { it.associatedAddress }
            }

            map { device ->
                val airdrop = allRelatedContacts[device.address]?.let {
                    AppleAirDrop(it.map { it.toDomain() })
                }

                device.toDomain(airdrop)
            }
        }
    }
}