package f.cking.software.domain.interactor

import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.domain.interactor.filterchecker.FilterCheckerImpl

class DeleteRadarProfile(
    private val radarProfilesRepository: RadarProfilesRepository,
    private val filterChecker: FilterCheckerImpl,
) {
    suspend fun execute(profileId: Int) {
        radarProfilesRepository.deleteProfile(profileId)
        filterChecker.clearCache()
    }
}