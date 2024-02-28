package f.cking.software.domain.model

import android.content.Context
import f.cking.software.getTimePeriodStr

data class DeviceData(
    val address: String,
    val name: String?,
    val lastDetectTimeMs: Long,
    val firstDetectTimeMs: Long,
    val manufacturerInfo: ManufacturerInfo?,
    val detectCount: Int,
    val customName: String?,
    val favorite: Boolean,
    val tags: Set<String>,
    val lastFollowingDetectionTimeMs: Long?,
    val rssi: Int?,
) {

    fun buildDisplayName(): String {
        return if (!customName.isNullOrBlank()) customName else name ?: address
    }

    fun firstDetectionPeriod(context: Context): String {
        return (System.currentTimeMillis() - firstDetectTimeMs).getTimePeriodStr(context)
    }

    fun lastDetectionPeriod(context: Context): String {
        return (System.currentTimeMillis() - lastDetectTimeMs).getTimePeriodStr(context)
    }

    fun hasBeenSeenTimeAgo(): Long {
        return System.currentTimeMillis() - lastDetectTimeMs
    }

    fun distance(): Float? {
        return if (rssi != null) {
            val txPower = -59 //hard coded power value. Usually ranges between -59 to -65
            val ratio = rssi * 1.0 / txPower
            val distance = if (ratio < 1.0) {
                Math.pow(ratio, 10.0)
            } else {
                (0.89976) * Math.pow(ratio, 7.7095) + 0.111
            }
            distance.toFloat()
        } else {
            null
        }
    }

    fun mergeWithNewDetected(new: DeviceData): DeviceData {
        return this.copy(
            detectCount = detectCount + 1,
            lastDetectTimeMs = new.lastDetectTimeMs,
            name = new.name,
            manufacturerInfo = new.manufacturerInfo,
            rssi = new.rssi,
        )
    }
}