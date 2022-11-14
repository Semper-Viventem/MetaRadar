package f.cking.software.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RadarProfile(
    val id: Int,
    val name: String,
    val description: String?,
    val isActive: Boolean = true,
    val detectFilter: Filter?,
) {

    @Serializable
    sealed class Filter {

        abstract fun check(device: DeviceData): Boolean

        @Serializable
        @SerialName("last_detection_interval")
        data class LastDetectionInterval(val from: Long, val to: Long) : Filter() {
            override fun check(device: DeviceData): Boolean {
                return device.lastDetectTimeMs in from..to
            }
        }

        @Serializable
        @SerialName("first_detection_interval")
        data class FirstDetectionInterval(val from: Long, val to: Long) : Filter() {
            override fun check(device: DeviceData): Boolean {
                return device.firstDetectTimeMs in from..to
            }
        }

        @Serializable
        @SerialName("name")
        data class Name(val name: String, val ignoreCase: Boolean) : Filter() {
            override fun check(device: DeviceData): Boolean {
                return device.name != null && device.name.contains(name, ignoreCase = ignoreCase)
            }
        }

        @Serializable
        @SerialName("address")
        data class Address(val address: String) : Filter() {
            override fun check(device: DeviceData): Boolean {
                return device.address == address
            }
        }

        @Serializable
        @SerialName("manufacturer")
        data class Manufacturer(val manufacturerId: Int) : Filter() {
            override fun check(device: DeviceData): Boolean {
                return device.manufacturerInfo?.id?.let { it == manufacturerId } ?: false
            }
        }

        @Serializable
        @SerialName("is_favorite")
        data class IsFavorite(val favorite: Boolean) : Filter() {
            override fun check(device: DeviceData): Boolean {
                return device.favorite == favorite
            }
        }

        @Serializable
        @SerialName("min_lost_time")
        data class MinLostTime(val minLostTime: Long) : Filter() {
            override fun check(device: DeviceData): Boolean {
                return System.currentTimeMillis() - device.lastDetectTimeMs >= minLostTime
            }
        }

        @Serializable
        @SerialName("any")
        data class Any(val filters: List<Filter>) : Filter() {
            override fun check(device: DeviceData): Boolean {
                return filters.any { it.check(device) }
            }
        }

        @Serializable
        @SerialName("all")
        data class All(val filters: List<Filter>) : Filter() {
            override fun check(device: DeviceData): Boolean {
                return filters.all { it.check(device) }
            }
        }

        @Serializable
        @SerialName("not")
        data class Not(val filter: Filter) : Filter() {
            override fun check(device: DeviceData): Boolean {
                return !filter.check(device)
            }
        }
    }
}