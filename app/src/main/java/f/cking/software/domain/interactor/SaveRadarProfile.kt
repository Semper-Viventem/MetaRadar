package f.cking.software.domain.interactor

import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.domain.interactor.filterchecker.FilterCheckerImpl
import f.cking.software.domain.model.RadarProfile

class SaveRadarProfile(
    private val radarProfilesRepository: RadarProfilesRepository,
    private val filterChecker: FilterCheckerImpl,
) {
    suspend fun execute(radarProfile: RadarProfile) {
        radarProfilesRepository.saveProfile(radarProfile)
        filterChecker.clearCache()
    }
}