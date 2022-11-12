package f.cking.software.domain.interactor

import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.repo.DevicesRepository

class SaveScanBatchInteractor(
    private val devicesRepository: DevicesRepository
) {

    fun execute(batch: List<BleScanDevice>) {
        devicesRepository.saveScanBatch(batch)
    }
}