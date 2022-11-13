package f.cking.software.domain.interactor

import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.model.DeviceData

class IsWantedDeviceInteractor(
    private val settingsRepository: SettingsRepository,
    private val isKnownDeviceInteractor: IsKnownDeviceInteractor
) {

    fun execute(
        deviceData: DeviceData,
        detectionTimeMs: Long,
        shouldBeFavorite: Boolean = true,
    ): Boolean {
        return isKnownDeviceInteractor.execute(deviceData)
                && detectionTimeMs - deviceData.lastDetectTimeMs >= settingsRepository.getWantedDevicePeriod()
                && ((shouldBeFavorite && deviceData.favorite) || !shouldBeFavorite)
    }
}