package f.cking.software.domain.interactor

import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.repo.DevicesRepository
import f.cking.software.domain.repo.SettingsRepository

class ClearGarbageInteractor(
    private val devicesRepository: DevicesRepository,
    private val settingsRepository: SettingsRepository,
    private val isKnownDeviceInteractor: IsKnownDeviceInteractor,
) {

    fun execute() {
        val devices = devicesRepository.getDevices()
        devices.forEach { device ->
            if (isGarbage(device)) {
                devicesRepository.deleteDevice(device)
            }
        }
    }

    /**
     * 1. Short visible period (< 1 hour by default)
     * 2. Last seen 1 day ago
     * 3. No public name
     */
    private fun isGarbage(device: DeviceData): Boolean {
        return !isKnownDeviceInteractor.execute(device)
                && System.currentTimeMillis() - device.lastDetectTimeMs > settingsRepository.getGarbagingTIme()
                && device.name == null
    }
}