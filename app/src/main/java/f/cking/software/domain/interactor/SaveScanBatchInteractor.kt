package f.cking.software.domain.interactor

import android.location.Location
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.data.repo.LocationRepository
import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
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

            val location = getFreshLocation()

            val detectTime = batch.firstOrNull()?.scanTimeMs
            if (location != null && detectTime != null) {
                locationRepository.saveLocation(location.toDomain(detectTime), batch.map { it.address })
            }
        }
    }

    private suspend fun getFreshLocation(): Location? {
        return locationProvider.observeLocation()
            .firstOrNull()
            ?.takeIf { it.isFresh() }
            ?.location
    }

    private fun LocationProvider.LocationHandle.isFresh(): Boolean {
        return System.currentTimeMillis() - this.emitTime < ALLOWED_LOCATION_LIVETIME_MS
    }

    companion object {
        private const val ALLOWED_LOCATION_LIVETIME_MS = 2L * 60L * 1000L // 2 min
    }
}