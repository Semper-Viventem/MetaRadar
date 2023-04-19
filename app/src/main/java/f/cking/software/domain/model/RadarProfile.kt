package f.cking.software.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class RadarProfile(
    val id: Int?,
    val name: String,
    val description: String?,
    val isActive: Boolean = true,
    val detectFilter: Filter?,
) {

    @Serializable
    sealed class Filter(@Transient protected val checkDifficulty: Int = 0) {

        open fun getDifficulty(): Int = checkDifficulty

        @Serializable
        @SerialName("last_detection_interval")
        data class LastDetectionInterval(val from: Long, val to: Long) : Filter()

        @Serializable
        @SerialName("first_detection_interval")
        data class FirstDetectionInterval(val from: Long, val to: Long) : Filter()

        @Serializable
        @SerialName("name")
        data class Name(val name: String, val ignoreCase: Boolean) : Filter()

        @Serializable
        @SerialName("address")
        data class Address(val address: String) : Filter()

        @Serializable
        @SerialName("manufacturer")
        data class Manufacturer(val manufacturerId: Int) : Filter()

        @Serializable
        @SerialName("is_favorite")
        data class IsFavorite(val favorite: Boolean) : Filter()

        @Serializable
        @SerialName("min_lost_time")
        data class MinLostTime(val minLostTime: Long) : Filter()

        @Serializable
        @SerialName("airdrop_contact")
        data class AppleAirdropContact(
            val contactStr: String,
            val airdropShaFormat: Int,
            val minLostTime: Long? = null,
        ) : Filter(checkDifficulty = 20)

        @Serializable
        @SerialName("is_following")
        data class IsFollowing(
            val followingDurationMs: Long,
            val followingDetectionIntervalMs: Long,
        ) : Filter(checkDifficulty = 30)

        @Serializable
        @SerialName("any")
        data class Any(val filters: List<Filter>) : Filter(checkDifficulty = 1) {
            override fun getDifficulty(): Int {
                return filters.sumOf { it.getDifficulty() } + checkDifficulty
            }
        }

        @Serializable
        @SerialName("all")
        data class All(val filters: List<Filter>) : Filter(checkDifficulty = 1) {
            override fun getDifficulty(): Int {
                return filters.sumOf { it.getDifficulty() } + checkDifficulty
            }
        }

        @Serializable
        @SerialName("not")
        data class Not(val filter: Filter) : Filter(checkDifficulty = 1) {
            override fun getDifficulty(): Int {
                return filter.getDifficulty() + checkDifficulty
            }
        }

        @Serializable
        @SerialName("device_location")
        data class DeviceLocation(
            val location: LocationModel,
            val radiusMeters: Float,
            val fromTimeMs: Long,
            val toTimeMs: Long,
        ) : Filter(checkDifficulty = 100)

        @Serializable
        @SerialName("user_location")
        data class UserLocation(
            val location: LocationModel,
            val radiusMeters: Float,
            val noLocationDefaultValue: Boolean,
        ) : Filter(checkDifficulty = 50)
    }
}