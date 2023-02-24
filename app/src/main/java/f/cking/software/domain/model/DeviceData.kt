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
    val lastFollowingDetectionTimeMs: Long?,
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

    fun mergeWithNewDetected(new: DeviceData): DeviceData {
        return this.copy(
            detectCount = detectCount + 1,
            lastDetectTimeMs = new.lastDetectTimeMs,
            name = new.name,
            manufacturerInfo = new.manufacturerInfo,
        )
    }
}