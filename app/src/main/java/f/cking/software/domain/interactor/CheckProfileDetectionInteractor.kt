package f.cking.software.domain.interactor

import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.model.RadarProfile

class CheckProfileDetectionInteractor(
    private val devicesRepository: DevicesRepository,
    private val radarProfilesRepository: RadarProfilesRepository,
    private val buildDeviceFromScanDataInteractor: BuildDeviceFromScanDataInteractor,
) {

    suspend fun execute(batch: List<BleScanDevice>): List<RadarProfile> {
        val existingDevices = devicesRepository.getAllByAddresses(batch.map { it.address })
        val foundDevices = batch.mapNotNull { found ->
            found.takeIf { existingDevices.none { existing -> found.address == existing.address } }?.let {
                buildDeviceFromScanDataInteractor.execute(it)
            }
        }
        val devices = existingDevices + foundDevices
        val allProfiles = radarProfilesRepository.getAllProfiles()
        return allProfiles.filter { profile ->
            profile.isActive && devices.any { profile.detectFilter?.check(it) == true }
        }
    }
}