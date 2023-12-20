package f.cking.software.domain.interactor

import f.cking.software.data.repo.SettingsRepository
import java.util.concurrent.TimeUnit

class GetAppUsageDaysInteractor(
    private val settingsRepository: SettingsRepository,
) {

    fun execute(): Long {
        val firstLaunchTime = settingsRepository.getFirstAppLaunchTime()

        if (firstLaunchTime == SettingsRepository.NO_APP_LAUNCH_TIME) {
            return 0
        }

        return (System.currentTimeMillis() - firstLaunchTime) / TimeUnit.DAYS.toMillis(1)
    }
}