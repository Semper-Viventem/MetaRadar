package f.cking.software.domain.interactor

import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.SettingsRepository
import java.util.concurrent.TimeUnit

class CheckNeedToShowEnjoyTheAppInteractor(
    private val settingsRepository: SettingsRepository,
    private val devicesRepository: DevicesRepository,
) {

    suspend fun execute(): Boolean {
        return !settingsRepository.getEnjoyTheAppAnswered()
                && getEnjoyTheAppStartPointDays() >= ENJOY_THE_APP_DAYS_FROM_START_POINT
                && checkMinDevices()
    }

    private suspend fun checkMinDevices(): Boolean {
        return devicesRepository.getPaginated(0, MIN_DEVICES_FOR_ENJOY_THE_APP).size >= MIN_DEVICES_FOR_ENJOY_THE_APP
    }

    private fun getEnjoyTheAppStartPointDays(): Long {
        val startPoint = settingsRepository.getEnjoyTheAppStartingPoint()

        if (startPoint == SettingsRepository.NO_ENJOY_THE_APP_STARTING_POINT) {
            return 0
        }

        return (System.currentTimeMillis() - startPoint) / TimeUnit.DAYS.toMillis(1)
    }

    companion object {
        private const val ENJOY_THE_APP_DAYS_FROM_START_POINT = 3
        private const val MIN_DEVICES_FOR_ENJOY_THE_APP = 30
    }
}