package f.cking.software.domain.interactor

import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.LocationRepository
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.model.DeviceData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClearGarbageInteractor(
    private val devicesRepository: DevicesRepository,
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
    private val isKnownDeviceInteractor: IsKnownDeviceInteractor,
) {

    suspend fun execute(): Int {
        return withContext(Dispatchers.Default) {
            val devices = devicesRepository.getDevices(withAirdropInfo = false)
                .asSequence()
                .filter { isGarbage(it) }
                .map { it.address }
                .toList()

            devicesRepository.deleteAllByAddress(devices)
            devicesRepository.clearUnAssociatedAirdrops()
            locationRepository.removeDeviceLocationsByAddresses(devices)
            devices.count()
        }
    }

    /**
     * 1. Short visible period (< 1 hour by default)
     * 2. Last seen 1 day ago
     * 3. No public name
     */
    private fun isGarbage(device: DeviceData): Boolean {
        return !isKnownDeviceInteractor.execute(device)
                && (System.currentTimeMillis() - device.lastDetectTimeMs) > settingsRepository.getGarbagingTime()
                && device.resolvedName == null
    }
}