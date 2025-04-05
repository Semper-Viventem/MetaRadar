package f.cking.software.domain.interactor

import f.cking.software.data.repo.LocationRepository
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.LocationModel
import timber.log.Timber

class CheckDeviceLocationHistoryInteractor(
    private val locationRepository: LocationRepository,
) {
    /**
     * @param targetLocation location to check
     * @param radius radius in meters
     * @param deviceAddress device address
     * @param fromTime in milliseconds
     * @param toTime in milliseconds
     *
     * @return true if device was detected in allowed radius from current location
     */
    suspend fun execute(
        targetLocation: LocationModel,
        radius: Float,
        device: DeviceData,
        fromTime: Long,
        toTime: Long,
    ): Boolean {
        Timber.tag(TAG).d("Checking device location history for device: ${device.address}")

        if (toTime < device.firstDetectTimeMs || fromTime > device.lastDetectTimeMs) {
            Timber.tag(TAG).d("Time didn't match: ${device.address}")
            return false
        }

        val locations =
            locationRepository.getAllLocationsByAddress(
                deviceAddress = device.address,
                fromTime = fromTime,
                toTime = toTime,
            )

        Timber.tag(TAG).d("Locations count: ${locations.size}, device: ${device.address}")

        return locations.any { location ->
            location.distanceTo(targetLocation) <= radius
        }
    }

    companion object {
        private const val TAG = "CheckDeviceLocationHistoryInteractor"
    }
}
