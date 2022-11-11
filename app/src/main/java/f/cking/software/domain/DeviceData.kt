package f.cking.software.domain

import f.cking.software.getTimePeriodStr

data class DeviceData(
    val address: String,
    val name: String?,
    val lastDetectTimeMs: Long,
    val firstDetectTimeMs: Long,
    val detectCount: Int,
    val customName: String?,
    val favorite: Boolean
) {

    fun isKnownDevice(): Boolean {
        return lastDetectTimeMs - firstDetectTimeMs > KNOWN_DEVICE_PERIOD_MS
    }

    fun isInterestingForDetection(
        detectionTimeMs: Long,
        minTimeToDetectMs: Long = MIN_TIME_TO_DETECT,
        shouldBeFavorite: Boolean = true,
    ): Boolean {
        return isKnownDevice()
                && detectionTimeMs - lastDetectTimeMs >= minTimeToDetectMs
                && ((shouldBeFavorite && favorite) || !shouldBeFavorite)
    }

    fun buildDisplayName(): String {
        return if (!customName.isNullOrBlank()) customName else name ?: address
    }

    fun firstDetectionPeriod(): String {
        return getTimePeriodStr(System.currentTimeMillis() - firstDetectTimeMs)
    }

    fun lastDetectionPeriod(): String {
        return getTimePeriodStr(System.currentTimeMillis() - lastDetectTimeMs)
    }

    companion object {
        private const val KNOWN_DEVICE_PERIOD_MS = 1000L * 60L * 60L // 1 hour
        private const val MIN_TIME_TO_DETECT = 1000L * 60L * 60L // 1 hour
    }
}