package f.cking.software.domain.model

import android.content.Context
import f.cking.software.dateTimeStringFormatLocalized
import f.cking.software.domain.interactor.BuildDeviceClassFromSystemInfo
import f.cking.software.domain.interactor.BuildExtendedAddressInfoInteractor
import f.cking.software.getTimePeriodStr
import java.time.format.FormatStyle

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
    val systemAddressType: Int?,
    val deviceClass: Int?,
    val isPaired: Boolean,
    val servicesUuids: List<String>,
    val rowDataEncoded: String?,
    val metadata: DeviceMetadata?,
    val isConnectable: Boolean,
) {
    val resolvedDeviceClass: DeviceClass by lazy {
        BuildDeviceClassFromSystemInfo.execute(this)
    }

    val resolvedName: String? by lazy {
        metadata?.buildDisplayName()?.takeIf { it.isNotBlank() } ?: name
    }

    val resolvedManufacturerName by lazy {
        metadata?.manufacturerName?.takeIf { it.isNotBlank() } ?: manufacturerInfo?.name
    }

    fun knownLifetime(): Long = lastDetectTimeMs - firstDetectTimeMs

    fun buildDisplayName(): String =
        customName?.takeIf { it.isNotBlank() }
            ?: name
            ?: metadata?.buildDisplayName()?.takeIf { it.isNotBlank() }
            ?: address

    fun firstDetectionPeriod(context: Context): String = (System.currentTimeMillis() - firstDetectTimeMs).getTimePeriodStr(context)

    fun firstDetectionExactTime(
        context: Context,
        formatStyle: FormatStyle = FormatStyle.SHORT,
    ): String = firstDetectTimeMs.dateTimeStringFormatLocalized(formatStyle)

    fun lastDetectionPeriod(context: Context): String = (System.currentTimeMillis() - lastDetectTimeMs).getTimePeriodStr(context)

    fun lastDetectionExactTime(
        context: Context,
        formatStyle: FormatStyle = FormatStyle.SHORT,
    ): String = lastDetectTimeMs.dateTimeStringFormatLocalized(formatStyle)

    fun hasBeenSeenTimeAgo(): Long = System.currentTimeMillis() - lastDetectTimeMs

    fun extendedAddressInfo(): ExtendedAddressInfo = BuildExtendedAddressInfoInteractor.execute(this)

    fun distance(): Float? =
        if (rssi != null) {
            val txPower = -59 // hard coded power value. Usually ranges between -59 to -65
            val ratio = rssi * 1.0 / txPower
            val distance =
                if (ratio < 1.0) {
                    Math.pow(ratio, 10.0)
                } else {
                    (0.89976) * Math.pow(ratio, 7.7095) + 0.111
                }
            distance.toFloat()
        } else {
            null
        }

    fun mergeWithNewDetected(new: DeviceData): DeviceData =
        this.copy(
            detectCount = detectCount + 1,
            lastDetectTimeMs = new.lastDetectTimeMs,
            name = new.name,
            manufacturerInfo = new.manufacturerInfo,
            rssi = new.rssi,
            systemAddressType = new.systemAddressType,
            isPaired = new.isPaired,
            deviceClass = new.deviceClass,
            servicesUuids = new.servicesUuids,
            rowDataEncoded = new.rowDataEncoded,
            isConnectable = new.isConnectable,
        )
}
