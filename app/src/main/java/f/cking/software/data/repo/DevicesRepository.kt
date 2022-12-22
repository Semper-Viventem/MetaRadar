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
            deviceDao.getAll().toDomainWithAirDrop()
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
            saveDevices(devices)
            saveContacts(devices)
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

    suspend fun saveFollowingDetection(device: DeviceData, detectionTime: Long) {
        withContext(Dispatchers.IO) {
            val new = device.copy(lastFollowingDetectionTimeMs = detectionTime)
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
            deviceDao.findAllByAddresses(addresses).toDomainWithAirDrop()
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
            val mappedDevices = mergeWithExistingDevices(devices).map { it.toData() }
            deviceDao.insertAll(mappedDevices)
        }
    }

    private suspend fun saveContacts(devices: List<DeviceData>) {
        withContext(Dispatchers.IO) {
            val addressesMap = devices.flatMap { device ->
                device.manufacturerInfo?.airdrop?.contacts?.map { it.sha256 to device.address } ?: emptyList()
            }.toMap()
            val mergedContacts =
                mergeWithExisting(devices.flatMap { it.manufacturerInfo?.airdrop?.contacts ?: emptyList() })
            val mappedContacts =
                mergedContacts.mapNotNull { contact -> addressesMap[contact.sha256]?.let { contact.toData(it) } }
            appleContactsDao.insertAll(mappedContacts)
        }
    }

    private suspend fun notifyListeners() {
        val data = getDevices()
        allDevices.emit(data)
    }

    private suspend fun mergeWithExistingDevices(devices: List<DeviceData>): List<DeviceData> {
        val existingDevices = getAllByAddresses(devices.map { it.address })
        return devices.map { device ->
            val existing = existingDevices.firstOrNull { it.address == device.address }
            existing?.mergeWithNewDetected(device) ?: device
        }
    }

    private suspend fun mergeWithExisting(
        contacts: List<AppleAirDrop.AppleContact>,
    ): List<AppleAirDrop.AppleContact> {
        val existingContacts = getAllBySHA(contacts.map { it.sha256 })
        return contacts.map { contact ->
            val existing = existingContacts.firstOrNull { it.sha256 == contact.sha256 }
            existing?.mergeWithNewContact(contact) ?: contact
        }
    }

    private suspend fun DeviceEntity.toDomainWithAirDrop(): DeviceData {
        return withContext(Dispatchers.IO) {
            val contacts = appleContactsDao.getByAddress(address)
            toDomain(AppleAirDrop(contacts.map { it.toDomain() }))
        }
    }

    private suspend fun List<DeviceEntity>.toDomainWithAirDrop(): List<DeviceData> {
        return withContext(Dispatchers.IO) {
            val allRelatedContacts = appleContactsDao.getByAddresses(map { it.address })
            map { device ->

                val airdrop = allRelatedContacts.asSequence()
                    .filter { it.associatedAddress == device.address }
                    .map { it.toDomain() }
                    .toList()
                    .takeIf { it.isNotEmpty() }
                    ?.let { AppleAirDrop(it) }

                device.toDomain(airdrop)
            }
        }
    }
}