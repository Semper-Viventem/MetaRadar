package f.cking.software.domain.interactor

import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.repo.DevicesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetKnownDevicesInteractor(
    private val devicesRepository: DevicesRepository,
    private val isKnownDeviceInteractor: IsKnownDeviceInteractor,
) {

    suspend fun execute(): List<DeviceData> {
        return withContext(Dispatchers.Default) {
            devicesRepository.getDevices().filter { device -> isKnownDeviceInteractor.execute(device) }
        }
    }
}