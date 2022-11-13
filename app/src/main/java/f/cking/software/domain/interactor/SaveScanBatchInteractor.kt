package f.cking.software.domain.interactor

import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.repo.DevicesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveScanBatchInteractor(
    private val devicesRepository: DevicesRepository,
    private val getManufacturerInfoFromRawBleInteractor: GetManufacturerInfoFromRawBleInteractor,
) {

    suspend fun execute(batch: List<BleScanDevice>) {
        withContext(Dispatchers.IO) {
            devicesRepository.saveScanBatch(batch.map { buildDeviceData(it) })
        }
    }

    private fun buildDeviceData(scanData: BleScanDevice): DeviceData {
        return DeviceData(
            address = scanData.address,
            name = scanData.name,
            lastDetectTimeMs = scanData.scanTimeMs,
            firstDetectTimeMs = scanData.scanTimeMs,
            detectCount = 1,
            customName = null,
            favorite = false,
            manufacturerInfo = scanData.scanRecordRaw?.let { getManufacturerInfoFromRawBleInteractor.execute(it) },
        )
    }
}