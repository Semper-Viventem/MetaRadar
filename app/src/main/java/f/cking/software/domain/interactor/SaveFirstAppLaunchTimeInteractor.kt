package f.cking.software.domain.interactor

import f.cking.software.data.repo.SettingsRepository

class SaveFirstAppLaunchTimeInteractor(
    private val settingsRepository: SettingsRepository
) {

    fun execute() {
        val time = System.currentTimeMillis()

        if (settingsRepository.getFirstAppLaunchTime() == SettingsRepository.NO_APP_LAUNCH_TIME) {
            settingsRepository.setFirstAppLaunchTime(time)
        }

        if (settingsRepository.getEnjoyTheAppStartingPoint() == SettingsRepository.NO_ENJOY_THE_APP_STARTING_POINT) {
            settingsRepository.setEnjoyTheAppStartingPoint(time)
        }
    }
}