package f.cking.software.domain.interactor

import f.cking.software.data.repo.SettingsRepository

class EnjoyTheAppAskLaterInteractor constructor(
    private val settingsRepository: SettingsRepository,
) {

    fun execute() {
        settingsRepository.setEnjoyTheAppStartingPoint(System.currentTimeMillis())
    }
}