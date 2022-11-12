package f.cking.software.domain.interactor

import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.repo.DevicesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveScanBatchInteractor(
    private val devicesRepository: DevicesRepository
) {

    suspend fun execute(batch: List<BleScanDevice>) {
        withContext(Dispatchers.IO) {
            devicesRepository.saveScanBatch(batch)
        }
    }
}