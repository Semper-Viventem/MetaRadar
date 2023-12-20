package f.cking.software.domain.interactor

import f.cking.software.data.repo.SettingsRepository

class SaveFirstAppLaunchTimeInteractor(
    private val settingsRepository: SettingsRepository
) {

    fun execute() {
        if (settingsRepository.getFirstAppLaunchTime() == SettingsRepository.NO_APP_LAUNCH_TIME) {
            settingsRepository.setFirstAppLaunchTime(System.currentTimeMillis())
        }
    }
}