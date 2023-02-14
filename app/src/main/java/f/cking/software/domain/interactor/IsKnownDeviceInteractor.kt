package f.cking.software.domain.interactor

import f.cking.software.TheAppConfig
import f.cking.software.domain.model.DeviceData

class IsKnownDeviceInteractor {

    fun execute(device: DeviceData): Boolean {
        return device.lastDetectTimeMs - device.firstDetectTimeMs > TheAppConfig.DEFAULT_KNOWN_DEVICE_PERIOD_MS
    }
}