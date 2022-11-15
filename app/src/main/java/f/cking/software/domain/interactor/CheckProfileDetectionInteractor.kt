package f.cking.software.domain.interactor

import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.RadarProfile

class CheckProfileDetectionInteractor(
    private val devicesRepository: DevicesRepository,
    private val radarProfilesRepository: RadarProfilesRepository,
    private val buildDeviceFromScanDataInteractor: BuildDeviceFromScanDataInteractor,
) {

    suspend fun execute(batch: List<BleScanDevice>): List<ProfileResult> {
        val existingDevices = devicesRepository.getAllByAddresses(batch.map { it.address })
        val foundDevices = batch.mapNotNull { found ->
            found.takeIf { existingDevices.none { existing -> found.address == existing.address } }?.let {
                buildDeviceFromScanDataInteractor.execute(it)
            }
        }
        val devices = existingDevices + foundDevices
        val allProfiles = radarProfilesRepository.getAllProfiles()

        return allProfiles.mapNotNull { profile ->
            val matchedDevice = devices.filter { profile.detectFilter?.check(it) == true }
            if (matchedDevice.isNotEmpty()) {
                ProfileResult(profile, matchedDevice)
            } else {
                null
            }
        }
    }

    data class ProfileResult(
        val profile: RadarProfile,
        val matched: List<DeviceData>,
    )
}