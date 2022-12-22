package f.cking.software.domain.interactor

import f.cking.software.data.repo.LocationRepository
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.LocationModel

class CheckDeviceIsFollowingInteractor(
    private val locationRepository: LocationRepository,
) {

    suspend fun execute(
        deviceData: DeviceData,
        minFollowingDuration: Long,
        followingDetectionInterval: Long,
    ): Boolean {
        val currentTime = System.currentTimeMillis()

        if (deviceData.lastFollowingDetectionTimeMs != null
            && currentTime - deviceData.lastFollowingDetectionTimeMs < followingDetectionInterval
        ) {
            return false
        }

        val lastLocations = locationRepository.getAllLocationsByAddress(deviceData.address)
            .filter { currentTime - it.time <= minFollowingDuration }

        if (lastLocations.isEmpty()) return false

        val distanceByAllSegments = distanceByAllSegments(lastLocations)
        val distanceBetweenFirstAndLast = lastLocations.first().distanceTo(lastLocations.last())

        return distanceByAllSegments >= MIN_DISTANCE_BY_ALL_SEGMENTS_M
                && distanceBetweenFirstAndLast >= MIN_DISTANCE_BETWEEN_FIRST_AND_LAST
    }

    private fun distanceByAllSegments(points: List<LocationModel>): Float {
        if (points.isEmpty() || points.size == 1) return 0f
        var result = 0f
        (1..points.lastIndex).forEach { i ->
            val distance = points[i - 1].distanceTo(points[i])
            result += distance
        }
        return result
    }

    companion object {
        private const val MIN_DISTANCE_BY_ALL_SEGMENTS_M = 300
        private const val MIN_DISTANCE_BETWEEN_FIRST_AND_LAST = 300
    }
}