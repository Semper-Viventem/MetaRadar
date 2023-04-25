package f.cking.software.domain.interactor

import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.LocationRepository

class ClearDeviceDataInteractor(
    private val devicesRepository: DevicesRepository,
    private val locationRepository: LocationRepository,
) {

    suspend fun execute(address: String) {
        devicesRepository.deleteAllByAddress(listOf(address))
        locationRepository.removeDeviceLocationsByAddresses(listOf(address))
    }
}