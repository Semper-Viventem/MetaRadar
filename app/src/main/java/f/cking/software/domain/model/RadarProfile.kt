package f.cking.software.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RadarProfile(
    val id: Int?,
    val name: String,
    val description: String?,
    val isActive: Boolean = true,
    val detectFilter: Filter?,
) {

    @Serializable
    sealed class Filter {

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
        ) : Filter()

        @Serializable
        @SerialName("is_following")
        data class IsFollowing(
            val followingDurationMs: Long,
            val followingDetectionIntervalMs: Long,
        ) : Filter()

        @Serializable
        @SerialName("any")
        data class Any(val filters: List<Filter>) : Filter()

        @Serializable
        @SerialName("all")
        data class All(val filters: List<Filter>) : Filter()

        @Serializable
        @SerialName("not")
        data class Not(val filter: Filter) : Filter()
    }
}