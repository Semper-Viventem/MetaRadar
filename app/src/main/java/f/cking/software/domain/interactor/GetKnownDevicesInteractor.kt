package f.cking.software.domain.interactor

import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.repo.DevicesRepository

class GetKnownDevicesInteractor(
    private val devicesRepository: DevicesRepository,
    private val isKnownDeviceInteractor: IsKnownDeviceInteractor,
) {

    fun execute(): List<DeviceData> {
        return devicesRepository.getDevices().filter { device -> isKnownDeviceInteractor.execute(device) }
    }
}