package f.cking.software.domain.interactor

import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CheckProfileDetectionInteractor(
    private val devicesRepository: DevicesRepository,
    private val radarProfilesRepository: RadarProfilesRepository,
    private val buildDeviceFromScanDataInteractor: BuildDeviceFromScanDataInteractor,
) {

    suspend fun execute(batch: List<BleScanDevice>): List<ProfileResult> {
        return withContext(Dispatchers.Default) {
            val existingDevices = devicesRepository.getAllByAddresses(batch.map { it.address })

            val devices = batch.map { found ->
                val mappedFound = buildDeviceFromScanDataInteractor.execute(found)
                val existing = existingDevices.firstOrNull { it.address == found.address }
                existing?.copy(manufacturerInfo = mapManufacturerInfo(mappedFound.manufacturerInfo)) ?: mappedFound
            }
            val allProfiles = radarProfilesRepository.getAllProfiles()

            allProfiles.mapNotNull { profile ->
                checkProfile(profile, devices)
            }
        }
    }

    private suspend fun mapManufacturerInfo(found: ManufacturerInfo?): ManufacturerInfo? {
        val airdrop = found?.airdrop ?: return found
        val existingContacts = devicesRepository.getAllBySHA(airdrop.contacts.map { it.sha256 })
        val mergedContacts = airdrop.contacts.map { contact ->
            val existing = existingContacts.firstOrNull { it.sha256 == contact.sha256 }
            existing ?: contact
        }
        return found.copy(airdrop = AppleAirDrop(mergedContacts))
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