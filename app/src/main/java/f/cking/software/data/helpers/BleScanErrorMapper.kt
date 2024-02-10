package f.cking.software.data.helpers

import android.bluetooth.le.ScanCallback

object BleScanErrorMapper {

    fun map(errorCode: Int): String {
        return when (errorCode) {
            ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Application registration failed. Possible solution is to restart the device."
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
            ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Out of hardware resources"
            ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> "Scanning too frequently"
            else -> "Unknown error"
        }
    }
}