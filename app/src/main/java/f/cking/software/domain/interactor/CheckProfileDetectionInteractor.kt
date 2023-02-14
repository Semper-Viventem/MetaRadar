package f.cking.software.domain.interactor

import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.RadarProfilesRepository
import f.cking.software.domain.interactor.filterchecker.FilterCheckerImpl
import f.cking.software.domain.model.*
import f.cking.software.domain.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CheckProfileDetectionInteractor(
    private val devicesRepository: DevicesRepository,
    private val radarProfilesRepository: RadarProfilesRepository,
    private val buildDeviceFromScanDataInteractor: BuildDeviceFromScanDataInteractor,
    private val filterChecker: FilterCheckerImpl,
    private val saveReportInteractor: SaveReportInteractor,
    private val locationProvider: LocationProvider,
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

            val result = allProfiles.mapNotNull { profile ->
                checkProfile(profile, devices)
            }

            result.forEach { saveReport(it) }

            result
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

    private suspend fun checkProfile(profile: RadarProfile, devices: List<DeviceData>): ProfileResult? {
        return profile.takeIf { it.isActive }
            ?.let { devices.filter { device -> profile.detectFilter?.let { filterChecker.check(device, it) } == true } }
            ?.takeIf { matched -> matched.isNotEmpty() }
            ?.let { matched -> ProfileResult(profile, matched) }
    }

    private suspend fun saveReport(result: ProfileResult) {
        val locationModel = locationProvider.getFreshLocation()

        val report = JournalEntry.Report.ProfileReport(
            profileId = result.profile.id ?: return,
            deviceAddresses = result.matched.map { it.address },
            locationModel = locationModel?.toDomain(System.currentTimeMillis()),
        )

        saveReportInteractor.execute(report)
    }

    data class ProfileResult(
        val profile: RadarProfile,
        val matched: List<DeviceData>,
    )
}