package f.cking.software.domain.repo

import f.cking.software.data.DeviceDao
import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.toData
import f.cking.software.domain.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DevicesRepository(
    private val deviceDao: DeviceDao,
    private val settingsRepository: SettingsRepository,
) {

    private val refs = hashMapOf<Int, Ref>()

    private var listeners: MutableSet<OnDevicesUpdateListener> = mutableSetOf()

    fun addListener(listener: OnDevicesUpdateListener) {
        listeners.add(listener)
        runBlocking {
            launch(Dispatchers.IO) {
                listener.onDevicesUpdate(getDevices())
            }
        }
    }

    fun removeListener(listener: OnDevicesUpdateListener) {
        listeners.remove(listener)
    }

    fun getDevices() = deviceDao.getAll().map { it.toDomain() }

    fun getKnownDevices(): List<DeviceData> {
        return getDevices().filter { device -> device.isKnownDevice(settingsRepository.getKnownDevicePeriod()) }
    }

    fun changeFavorite(device: DeviceData) {
        val new = device.copy(favorite = !device.favorite)
        deviceDao.update(new.toData())
        notifyListeners()
    }

    /**
     * @return count of known devices (device lifetime > 1 hour)
     */
    fun detectBatch(devices: List<BleScanDevice>): Result {
        val wantedDevices: MutableSet<DeviceData> = mutableSetOf()
        devices.forEach {
            detect(it)?.let { wantedDevices.add(it) }
        }
        notifyListeners()
        val knownDeviceCount = getKnownDevicesCount(devices.map { it.address })
        return Result(knownDeviceCount, wantedDevices)
    }

    data class Result(
        val knownDevicesCount: Int,
        val wanted: Set<DeviceData>,
    )

    private fun notifyListeners() {
        val data = getDevices()
        listeners.forEach { it.onDevicesUpdate(data) }
    }

    /**
     * @return Should return device if it's wanted
     */
    private fun detect(device: BleScanDevice): DeviceData? {
        val existing = deviceDao.findByAddress(device.address)?.toDomain()
        val isWanted = existing?.isInterestingForDetection(
            detectionTimeMs = device.scanTimeMs,
            knownDevicePeriodMs = settingsRepository.getKnownDevicePeriod(),
            minTimeToDetectMs = settingsRepository.getWantedDevicePeriod(),
        ) ?: false

        return if (existing != null) {
            val updated = updateExisting(existing, device)
            if (isWanted) {
                updated
            } else {
                null
            }
        } else {
            createNew(device)
            null
        }
    }

    private fun getKnownDevicesCount(addresses: List<String>): Int {
        return deviceDao.findAllByAddresses(addresses).count {
            it.toDomain().isKnownDevice(settingsRepository.getKnownDevicePeriod())
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

    private fun updateExisting(existing: DeviceData, device: BleScanDevice): DeviceData {
        val newData = existing.copy(
            detectCount = existing.detectCount + 1,
            lastDetectTimeMs = device.scanTimeMs,
        )
        deviceDao.update(newData.toData())
        return newData
    }

    private fun makeRelations(devices: List<BleScanDevice>) {
        devices.forEachIndexed { i, first ->
            ((i + 1)..(devices.lastIndex)).forEach { j ->
                val second = devices[j]
                findRef(first, second)
            }
        }
    }

    private fun findRef(first: BleScanDevice, second: BleScanDevice) {
        val nodes = hashSetOf(first.address, second.address)
        val hash = (nodes.first() + nodes.last()).hashCode()

        val newRef = Ref(hash, first = nodes.first(), second = nodes.last(), weight = 1)
        val existingRef = this.refs[hash]

        if (existingRef != null) {
            refs[hash] = existingRef.copy(weight = existingRef.weight + 1)
        } else {
            refs[hash] = newRef
        }
    }

    data class Ref(
        val refHash: Int,
        val first: String,
        val second: String,
        val weight: Int,
    )

    interface OnDevicesUpdateListener {
        fun onDevicesUpdate(devices: List<DeviceData>)
    }
}