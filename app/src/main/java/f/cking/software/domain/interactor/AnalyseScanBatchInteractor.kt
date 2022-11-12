package f.cking.software.domain.interactor

import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.repo.DevicesRepository

class AnalyseScanBatchInteractor(
    private val devicesRepository: DevicesRepository,
    private val isWantedDeviceInteractor: IsWantedDeviceInteractor,
    private val getKnownDevicesCountInteractor: GetKnownDevicesCountInteractor,
) {

    fun execute(batch: List<BleScanDevice>): Result {
        if (batch.isEmpty()) {
            return Result(knownDevicesCount = 0, wanted = emptySet())
        }
        val scanTime = batch.first().scanTimeMs
        val addresses = batch.map { it.address }
        val knownCount = getKnownDevicesCountInteractor.execute(addresses)
        val knownDevices = devicesRepository.getAllByAddresses(addresses)
        val wanted = knownDevices.filter { isWantedDeviceInteractor.execute(it, detectionTimeMs = scanTime) }

        return Result(
            knownDevicesCount = knownCount,
            wanted = wanted.toSet(),
        )
    }

    data class Result(
        val knownDevicesCount: Int,
        val wanted: Set<DeviceData>,
    )
}