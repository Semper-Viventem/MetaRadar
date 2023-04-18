package f.cking.software.domain.interactor

import f.cking.software.data.repo.LocationRepository
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.LocationModel

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

        if (toTime < device.firstDetectTimeMs || fromTime > device.lastDetectTimeMs) {
            return false
        }

        val locations = locationRepository.getAllLocationsByAddress(
            deviceAddress = device.address,
            fromTime = fromTime,
            toTime = toTime
        )

        return locations.any { location ->
            location.distanceTo(targetLocation) <= radius
        }
    }
}