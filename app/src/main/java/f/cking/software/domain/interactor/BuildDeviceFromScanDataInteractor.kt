package f.cking.software.domain.interactor

import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.model.DeviceData

class BuildDeviceFromScanDataInteractor(
    private val getManufacturerInfoFromRawBleInteractor: GetManufacturerInfoFromRawBleInteractor,
) {

    fun execute(scanData: BleScanDevice): DeviceData {
        return DeviceData(
            address = scanData.address,
            name = scanData.name,
            lastDetectTimeMs = scanData.scanTimeMs,
            firstDetectTimeMs = scanData.scanTimeMs,
            detectCount = 1,
            customName = null,
            favorite = false,
            manufacturerInfo = scanData.scanRecordRaw?.let {
                getManufacturerInfoFromRawBleInteractor.execute(
                    it,
                    scanData.scanTimeMs
                )
            },
            lastFollowingDetectionTimeMs = null,
            tags = emptySet(),
        )
    }
}