package f.cking.software.domain.interactor

import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.LocationRepository
import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveScanBatchInteractor(
    private val devicesRepository: DevicesRepository,
    private val locationRepository: LocationRepository,
    private val buildDeviceFromScanDataInteractor: BuildDeviceFromScanDataInteractor,
    private val locationProvider: LocationProvider,
) {

    suspend fun execute(batch: List<BleScanDevice>) {
        withContext(Dispatchers.Default) {
            devicesRepository.saveScanBatch(batch.map { buildDeviceFromScanDataInteractor.execute(it) })

            val location = locationProvider.getFreshLocation()

            val detectTime = batch.firstOrNull()?.scanTimeMs
            if (location != null && detectTime != null) {
                locationRepository.saveLocation(location.toDomain(detectTime), batch.map { it.address })
            }
        }
    }
}