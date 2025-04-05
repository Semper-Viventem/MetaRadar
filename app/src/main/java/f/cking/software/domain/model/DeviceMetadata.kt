package f.cking.software.domain.model

import f.cking.software.extract16BitUuid
import kotlinx.serialization.Serializable

/**
 * Optonal device metadata recieved from GATT services
 */
@Serializable
data class DeviceMetadata(
    val deviceName: String? = null,
    val manufacturerName: String? = null,
    val modelNumber: String? = null,
    val serialNumber: String? = null,
    val batteryLevel: Int? = null,
) {
    fun buildDisplayName(): String? =
        when {
            !deviceName.isNullOrEmpty() && modelNumber?.contains(deviceName) == true -> modelNumber
            else -> deviceName ?: modelNumber
        }

    enum class ServiceTypes(
        val uuid: String,
    ) {
        GENERIC_ACCESS("1800"),
        DEVICE_INFORMATION("180A"),
        BATTERY_SERVICE("180F"),
        ;

        companion object {
            val map = entries.associateBy(ServiceTypes::uuid)

            fun findByUuid(uuid: String): ServiceTypes? = map[extract16BitUuid(uuid)?.uppercase()]
        }
    }

    enum class CharacteristicType(
        val uuid: String,
    ) {
        DEVICE_NAME("2A00"),
        MANUFACTURER_NAME("2A29"),
        MODEL_NUMBER("2A24"),
        SERIAL_NUMBER("2A25"),
        BATTERY_LEVEL("2A19"),
        ;

        companion object {
            val map = entries.associateBy(CharacteristicType::uuid)

            fun findByUuid(uuid: String): CharacteristicType? = map[extract16BitUuid(uuid)?.uppercase()]
        }
    }
}

fun DeviceMetadata?.isNullOrEmpty(): Boolean {
    if (this == null) return true
    return deviceName.isNullOrEmpty() &&
        manufacturerName.isNullOrEmpty() &&
        modelNumber.isNullOrEmpty() &&
        serialNumber.isNullOrEmpty() &&
        batteryLevel == null
}
