package f.cking.software.domain.interactor

import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.repo.DevicesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnalyseScanBatchInteractor(
    private val devicesRepository: DevicesRepository,
    private val isWantedDeviceInteractor: IsWantedDeviceInteractor,
    private val getKnownDevicesCountInteractor: GetKnownDevicesCountInteractor,
) {

    suspend fun execute(batch: List<BleScanDevice>): Result {
        return withContext(Dispatchers.Default) {
            if (batch.isEmpty()) {
                Result(knownDevicesCount = 0, wanted = emptySet())
            } else {
                val scanTime = batch.first().scanTimeMs
                val addresses = batch.map { it.address }
                val knownCount = getKnownDevicesCountInteractor.execute(addresses)
                val knownDevices = devicesRepository.getAllByAddresses(addresses)
                val wanted = knownDevices.filter { isWantedDeviceInteractor.execute(it, detectionTimeMs = scanTime) }

                Result(
                    knownDevicesCount = knownCount,
                    wanted = wanted.toSet(),
                )
            }
        }
    }

    data class Result(
        val knownDevicesCount: Int,
        val wanted: Set<DeviceData>,
    )
}