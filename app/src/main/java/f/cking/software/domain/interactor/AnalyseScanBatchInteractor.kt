package f.cking.software.domain.interactor

import f.cking.software.domain.model.BleScanDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnalyseScanBatchInteractor(
    private val checkProfileDetectionInteractor: CheckProfileDetectionInteractor,
    private val getKnownDevicesCountInteractor: GetKnownDevicesCountInteractor,
) {

    suspend fun execute(batch: List<BleScanDevice>): Result {
        return withContext(Dispatchers.Default) {
            if (batch.isEmpty()) {
                Result(knownDevicesCount = 0, matchedProfiles = emptyList())
            } else {
                val scanTime = batch.first().scanTimeMs
                val addresses = batch.map { it.address }
                val knownCount = getKnownDevicesCountInteractor.execute(addresses)
                val matchedProfiles = checkProfileDetectionInteractor.execute(batch)

                Result(
                    knownDevicesCount = knownCount,
                    matchedProfiles = matchedProfiles,
                )
            }
        }
    }

    data class Result(
        val knownDevicesCount: Int,
        val matchedProfiles: List<CheckProfileDetectionInteractor.ProfileResult>,
    )
}