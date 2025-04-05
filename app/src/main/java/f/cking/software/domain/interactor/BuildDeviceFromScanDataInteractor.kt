package f.cking.software.domain.interactor

import f.cking.software.domain.model.BleScanDevice
import f.cking.software.domain.model.DeviceData
import f.cking.software.toBase64

class BuildDeviceFromScanDataInteractor(
    private val getManufacturerInfoFromRawBleInteractor: GetManufacturerInfoFromRawBleInteractor,
) {
    fun execute(scanData: BleScanDevice): DeviceData {
        val rawData = scanData.scanRecordRaw

        return DeviceData(
            address = scanData.address,
            name = scanData.name,
            lastDetectTimeMs = scanData.scanTimeMs,
            firstDetectTimeMs = scanData.scanTimeMs,
            detectCount = 1,
            customName = null,
            favorite = false,
            manufacturerInfo =
                rawData?.let {
                    getManufacturerInfoFromRawBleInteractor.execute(it, scanData.scanTimeMs)
                },
            lastFollowingDetectionTimeMs = null,
            tags = emptySet(),
            rssi = scanData.rssi,
            systemAddressType = scanData.addressType,
            deviceClass = scanData.deviceClass,
            isPaired = scanData.isPaired,
            servicesUuids = scanData.serviceUuids,
            rowDataEncoded = rawData?.toBase64(),
            metadata = null,
            isConnectable = scanData.isConnectable,
        )
    }
}
