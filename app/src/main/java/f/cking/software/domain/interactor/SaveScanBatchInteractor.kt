package f.cking.software.domain.interactor

import f.cking.software.data.repo.DevicesRepository
import f.cking.software.domain.model.BleScanDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveScanBatchInteractor(
    private val devicesRepository: DevicesRepository,
    private val buildDeviceFromScanDataInteractor: BuildDeviceFromScanDataInteractor,
) {

    suspend fun execute(batch: List<BleScanDevice>) {
        withContext(Dispatchers.Default) {
            devicesRepository.saveScanBatch(batch.map { buildDeviceFromScanDataInteractor.execute(it) })
        }
    }
}