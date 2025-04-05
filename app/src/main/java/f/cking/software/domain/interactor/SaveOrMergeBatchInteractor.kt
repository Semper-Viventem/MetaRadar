package f.cking.software.domain.interactor

import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.LocationRepository
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.model.AppleAirDrop
import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.domain.model.SavedDeviceHandle
import f.cking.software.domain.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveOrMergeBatchInteractor(
    private val devicesRepository: DevicesRepository,
    private val locationRepository: LocationRepository,
    private val buildDeviceFromScanDataInteractor: BuildDeviceFromScanDataInteractor,
    private val locationProvider: LocationProvider,
    private val isKnownDeviceInteractor: IsKnownDeviceInteractor,
    private val deviceServicesFetchingPlanner: DeviceServicesFetchingPlanner,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun execute(batch: List<BleScanDevice>): Result =
        withContext(Dispatchers.Default) {
            val discoveredDevices = batch.map { buildDeviceFromScanDataInteractor.execute(it) }
            val existingDevices = devicesRepository.getAllByAddresses(discoveredDevices.map { it.address }).associateBy { it.address }
            val airdropContactToPreviouslySeenAtTime = mutableMapOf<Int, Long>()

            val mergedDevices =
                discoveredDevices.map { newDiscovered ->
                    val existing = existingDevices[newDiscovered.address]
                    val mergedDeviceData = existing?.mergeWithNewDetected(newDiscovered) ?: newDiscovered
                    val airdropMergeResult = mergeAirdropContactsWithExisting(mergedDeviceData.manufacturerInfo)
                    airdropContactToPreviouslySeenAtTime.putAll(airdropMergeResult.airdropContactToPreviouslySeenAtTime)

                    mergedDeviceData.copy(manufacturerInfo = airdropMergeResult.updatedManufacturerInfo)
                }

            devicesRepository.saveScanBatch(mergedDevices)

            var savedBatch =
                mergedDevices.map { mergedDevice ->
                    SavedDeviceHandle(
                        previouslySeenAtTime = existingDevices[mergedDevice.address]?.lastDetectTimeMs ?: mergedDevice.lastDetectTimeMs,
                        device = mergedDevice,
                        airdrop =
                            airdropContactToPreviouslySeenAtTime
                                .takeIf { it.isNotEmpty() }
                                ?.let { SavedDeviceHandle.AirdropHandle(it) },
                    )
                }

            if (settingsRepository.getEnableDeepAnalysis()) {
                savedBatch = deviceServicesFetchingPlanner.scheduleFetchServiceInfo(savedBatch)
            }

            val location = locationProvider.getFreshLocation()

            val detectTime = batch.firstOrNull()?.scanTimeMs
            if (location != null && detectTime != null) {
                locationRepository.saveLocation(location.toDomain(detectTime), batch.map { it.address })
            }

            val knownDevicesCount = mergedDevices.count(isKnownDeviceInteractor::execute)
            Result(
                knownDevicesCount = knownDevicesCount,
                savedBatch = savedBatch,
            )
        }

    private suspend fun mergeAirdropContactsWithExisting(found: ManufacturerInfo?): AirdropContactsMergeResult {
        val airdrop = found?.airdrop ?: return AirdropContactsMergeResult(found, emptyMap())

        val airdropContactToPreviouslySeenAtTime = mutableMapOf<Int, Long>()
        val existingContacts = devicesRepository.getAllBySHA(airdrop.contacts.map { it.sha256 }).associateBy { it.sha256 }
        val mergedContacts =
            airdrop.contacts.map { contact ->
                val existing = existingContacts[contact.sha256]
                if (existing != null) {
                    airdropContactToPreviouslySeenAtTime[existing.sha256] = existing.lastDetectionTimeMs
                }
                existing?.mergeWithNewContact(contact) ?: contact
            }
        return AirdropContactsMergeResult(
            found.copy(airdrop = AppleAirDrop(mergedContacts)),
            airdropContactToPreviouslySeenAtTime,
        )
    }

    private data class AirdropContactsMergeResult(
        val updatedManufacturerInfo: ManufacturerInfo?,
        val airdropContactToPreviouslySeenAtTime: Map<Int, Long>,
    )

    data class Result(
        val knownDevicesCount: Int,
        val savedBatch: List<SavedDeviceHandle>,
    )

    companion object {
        private const val TAG = "SaveOrMergeBatchInteractor"
    }
}
