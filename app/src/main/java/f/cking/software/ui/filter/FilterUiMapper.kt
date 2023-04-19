package f.cking.software.ui.filter

import f.cking.software.SHA256
import f.cking.software.data.helpers.BluetoothSIG
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.domain.model.RadarProfile
import f.cking.software.timeFromDateTime
import f.cking.software.toLocalDate
import f.cking.software.toLocalTime
import java.time.LocalDate
import java.time.LocalTime

object FilterUiMapper {


    fun mapToDomain(from: FilterUiState): RadarProfile.Filter {
        return when (from) {
            is FilterUiState.Name -> RadarProfile.Filter.Name(from.name, from.ignoreCase)
            is FilterUiState.Address -> RadarProfile.Filter.Address(from.address)
            is FilterUiState.IsFavorite -> RadarProfile.Filter.IsFavorite(from.favorite)
            is FilterUiState.Manufacturer -> RadarProfile.Filter.Manufacturer(from.manufacturer!!.id)
            is FilterUiState.LastDetectionInterval -> RadarProfile.Filter.LastDetectionInterval(
                from = mapTimeToUi(from.fromDate, from.fromTime, Long.MIN_VALUE),
                to = mapTimeToUi(from.toDate, from.toTime, Long.MAX_VALUE),
            )
            is FilterUiState.FirstDetectionInterval -> RadarProfile.Filter.FirstDetectionInterval(
                from = mapTimeToUi(from.fromDate, from.fromTime, Long.MIN_VALUE),
                to = mapTimeToUi(from.toDate, from.toTime, Long.MAX_VALUE),
            )
            is FilterUiState.MinLostTime -> RadarProfile.Filter.MinLostTime(from.minLostTime!!)
            is FilterUiState.AppleAirdropContact -> RadarProfile.Filter.AppleAirdropContact(
                contactStr = from.contactString.trim(),
                airdropShaFormat = SHA256.fromStringAirdrop(from.contactString),
                minLostTime = from.minLostTime!!,
            )
            is FilterUiState.IsFollowing -> RadarProfile.Filter.IsFollowing(
                followingDurationMs = from.followingDurationMs,
                followingDetectionIntervalMs = from.followingDetectionIntervalMs,
            )
            is FilterUiState.DeviceLocation -> RadarProfile.Filter.DeviceLocation(
                location = from.targetLocation!!,
                radiusMeters = from.radius,
                fromTimeMs = mapTimeToUi(from.fromDate, from.fromTime, Long.MIN_VALUE),
                toTimeMs = mapTimeToUi(from.toDate, from.toTime, Long.MAX_VALUE),
            )
            is FilterUiState.UserLocation -> RadarProfile.Filter.UserLocation(
                location = from.targetLocation!!,
                radiusMeters = from.radius,
                noLocationDefaultValue = from.defaultValueIfNoLocation,
            )
            is FilterUiState.Any -> RadarProfile.Filter.Any(from.filters.map { mapToDomain(it) }.sortedBy { it.getDifficulty() })
            is FilterUiState.All -> RadarProfile.Filter.All(from.filters.map { mapToDomain(it) }.sortedBy { it.getDifficulty() })
            is FilterUiState.Not -> RadarProfile.Filter.Not(mapToDomain(from.filter!!))
            is FilterUiState.Unknown, is FilterUiState.Interval -> throw IllegalArgumentException("Unsupported type: ${from::class.java}")
        }
    }

    fun mapToUi(from: RadarProfile.Filter): FilterUiState {
        return when (from) {
            is RadarProfile.Filter.Name -> FilterUiState.Name().apply {
                this.name = from.name
                this.ignoreCase = from.ignoreCase
            }
            is RadarProfile.Filter.Address -> FilterUiState.Address().apply {
                this.address = from.address
            }
            is RadarProfile.Filter.Manufacturer -> FilterUiState.Manufacturer().apply {
                this.manufacturer = BluetoothSIG.bluetoothSIG[from.manufacturerId]?.let {
                    ManufacturerInfo(from.manufacturerId, it, null,)
                }
            }
            is RadarProfile.Filter.IsFavorite -> FilterUiState.IsFavorite().apply {
                this.favorite = from.favorite
            }
            is RadarProfile.Filter.FirstDetectionInterval -> FilterUiState.FirstDetectionInterval().apply {
                this.fromDate = from.from.takeIf { it != Long.MIN_VALUE }?.toLocalDate()
                this.fromTime = from.from.takeIf { it != Long.MIN_VALUE }?.toLocalTime()
                this.toDate = from.to.takeIf { it != Long.MAX_VALUE }?.toLocalDate()
                this.toTime = from.to.takeIf { it != Long.MAX_VALUE }?.toLocalTime()
            }
            is RadarProfile.Filter.LastDetectionInterval -> FilterUiState.LastDetectionInterval().apply {
                this.fromDate = from.from.takeIf { it != Long.MIN_VALUE }?.toLocalDate()
                this.fromTime = from.from.takeIf { it != Long.MIN_VALUE }?.toLocalTime()
                this.toDate = from.to.takeIf { it != Long.MAX_VALUE }?.toLocalDate()
                this.toTime = from.to.takeIf { it != Long.MAX_VALUE }?.toLocalTime()
            }
            is RadarProfile.Filter.MinLostTime -> FilterUiState.MinLostTime().apply {
                this.minLostTime = from.minLostTime
            }
            is RadarProfile.Filter.All -> FilterUiState.All().apply {
                this.filters = from.filters.map { mapToUi(it) }
            }
            is RadarProfile.Filter.Any -> FilterUiState.Any().apply {
                this.filters = from.filters.map { mapToUi(it) }
            }
            is RadarProfile.Filter.Not -> FilterUiState.Not().apply {
                this.filter = mapToUi(from.filter)
            }
            is RadarProfile.Filter.AppleAirdropContact -> FilterUiState.AppleAirdropContact().apply {
                this.contactString = from.contactStr
                this.minLostTime = from.minLostTime
            }
            is RadarProfile.Filter.IsFollowing -> FilterUiState.IsFollowing().apply {
                this.followingDurationMs = from.followingDurationMs
                this.followingDetectionIntervalMs = from.followingDetectionIntervalMs
            }
            is RadarProfile.Filter.DeviceLocation -> FilterUiState.DeviceLocation().apply {
                this.targetLocation = from.location
                this.radius = from.radiusMeters
                this.fromDate = from.fromTimeMs.takeIf { it != Long.MIN_VALUE }?.toLocalDate()
                this.fromTime = from.fromTimeMs.takeIf { it != Long.MIN_VALUE }?.toLocalTime()
                this.toDate = from.toTimeMs.takeIf { it != Long.MAX_VALUE }?.toLocalDate()
                this.toTime = from.toTimeMs.takeIf { it != Long.MAX_VALUE }?.toLocalTime()
            }
            is RadarProfile.Filter.UserLocation -> FilterUiState.UserLocation().apply {
                this.targetLocation = from.location
                this.radius = from.radiusMeters
                this.defaultValueIfNoLocation = from.noLocationDefaultValue
            }
        }
    }

    private fun mapTimeToUi(date: LocalDate?, time: LocalTime?, defaultValue: Long): Long {
        return if (date != null && time != null) {
            timeFromDateTime(date, time)
        } else {
            defaultValue
        }
    }
}