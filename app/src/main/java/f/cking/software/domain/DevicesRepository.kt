package f.cking.software.domain

import f.cking.software.data.DeviceDao

class DevicesRepository(
    private val deviceDao: DeviceDao,
) {

    private val refs = hashMapOf<Int, Ref>()

    fun detectBatch(devices: List<BleDevice>) {
        devices.forEach { detect(it) }
        makeRelations(devices)
    }

    fun detect(device: BleDevice) {
        val existing = deviceDao.findByAddress(device.address)?.toDomain()

        if (existing != null) {
            updateExisting(existing, device)
        } else {
            createNew(device)
        }
    }

    fun makeRelations(devices: List<BleDevice>) {
        devices.forEachIndexed { i, first ->
            ((i + 1)..(devices.lastIndex)).forEach { j ->
                val second = devices[j]
                findRef(first, second)
            }
        }
    }

    fun getDevices() = deviceDao.getAll().map { it.toDomain() }

    private fun findRef(first: BleDevice, second: BleDevice) {
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

    private fun createNew(device: BleDevice) {
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

    private fun updateExisting(existing: DeviceData, device: BleDevice) {
        val newData = existing.copy(
            detectCount = existing.detectCount + 1,
            lastDetectTimeMs = device.scanTimeMs,
        )
        deviceDao.update(newData.toData())
    }

    data class Ref(
        val refHash: Int,
        val first: String,
        val second: String,
        val weight: Int,
    )
}