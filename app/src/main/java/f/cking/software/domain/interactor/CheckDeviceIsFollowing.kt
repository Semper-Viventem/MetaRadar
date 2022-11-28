package f.cking.software.domain.interactor

import f.cking.software.data.repo.LocationRepository
import f.cking.software.domain.model.LocationModel

class CheckDeviceIsFollowing(
    private val locationRepository: LocationRepository,
) {

    suspend fun execute(deviceAddress: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastLocations = locationRepository.getAllLocationsByAddress(deviceAddress)
            .filter { currentTime - it.time <= FOLLOWING_DETECTION_TIME_MS }

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
        private const val FOLLOWING_DETECTION_TIME_MS = 15L * 60L * 1000L // 15 min
        private const val MIN_DISTANCE_BY_ALL_SEGMENTS_M = 300
        private const val MIN_DISTANCE_BETWEEN_FIRST_AND_LAST = 300
    }
}