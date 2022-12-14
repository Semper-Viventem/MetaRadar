package f.cking.software.domain.interactor

import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.model.DeviceData

class IsKnownDeviceInteractor(
    private val settingsRepository: SettingsRepository,
) {

    fun execute(device: DeviceData): Boolean {
        return device.lastDetectTimeMs - device.firstDetectTimeMs > settingsRepository.getKnownDevicePeriod()
    }
}