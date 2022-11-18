package f.cking.software.domain.interactor

import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.RadarProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CheckProfileDetectionInteractor(
    private val devicesRepository: DevicesRepository,
    private val radarProfilesRepository: RadarProfilesRepository,
    private val buildDeviceFromScanDataInteractor: BuildDeviceFromScanDataInteractor,
) {

    suspend fun execute(batch: List<BleScanDevice>): List<ProfileResult> {
        return withContext(Dispatchers.Default) {
            val existingDevices = devicesRepository.getAllByAddresses(batch.map { it.address }, withAirdropInfo = false)

            val devices = batch.map { found ->
                val mappedFound = buildDeviceFromScanDataInteractor.execute(found)
                val existing = existingDevices.firstOrNull { it.address == found.address }
                existing?.copy(manufacturerInfo = mappedFound.manufacturerInfo) ?: mappedFound
            }
            val allProfiles = radarProfilesRepository.getAllProfiles()

            allProfiles.mapNotNull { profile ->
                checkProfile(profile, devices)
            }
        }
    }

    private fun checkProfile(profile: RadarProfile, devices: List<DeviceData>): ProfileResult? {
        return profile.takeIf { it.isActive }
            ?.let { devices.filter { profile.detectFilter?.check(it) == true } }
            ?.takeIf { matched -> matched.isNotEmpty() }
            ?.let { matched -> ProfileResult(profile, matched) }
    }

    data class ProfileResult(
        val profile: RadarProfile,
        val matched: List<DeviceData>,
    )
}