package f.cking.software.ui.devicelist

import f.cking.software.*
import f.cking.software.data.helpers.BluetoothSIG
import f.cking.software.domain.model.ManufacturerInfo
import f.cking.software.domain.model.RadarProfile
import f.cking.software.ui.filter.FilterUiState
import java.util.*

object FilterUiMapper {


    fun mapToDomain(from: FilterUiState): RadarProfile.Filter {
        return when (from) {
            is FilterUiState.Name -> RadarProfile.Filter.Name(from.name, from.ignoreCase)
            is FilterUiState.Address -> RadarProfile.Filter.Address(from.address)
            is FilterUiState.IsFavorite -> RadarProfile.Filter.IsFavorite(from.favorite)
            is FilterUiState.Manufacturer -> RadarProfile.Filter.Manufacturer(from.manufacturer.get().id)
            is FilterUiState.Any -> RadarProfile.Filter.Any(from.filters.map { mapToDomain(it) })
            is FilterUiState.All -> RadarProfile.Filter.All(from.filters.map { mapToDomain(it) })
            is FilterUiState.Not -> RadarProfile.Filter.Not(mapToDomain(from.filter.get()))
            is FilterUiState.LastDetectionInterval -> RadarProfile.Filter.LastDetectionInterval(
                from = if (from.fromDate.isPresent && from.fromTime.isPresent) {
                    timeFromDateTime(from.fromDate.get(), from.fromTime.get())
                } else {
                    Long.MIN_VALUE
                },
                to = if (from.toDate.isPresent && from.toTime.isPresent) {
                    timeFromDateTime(from.toDate.get(), from.toTime.get())
                } else {
                    Long.MAX_VALUE
                }
            )
            is FilterUiState.FirstDetectionInterval -> RadarProfile.Filter.FirstDetectionInterval(
                from = if (from.fromDate.isPresent && from.fromTime.isPresent) {
                    timeFromDateTime(from.fromDate.get(), from.fromTime.get())
                } else {
                    Long.MIN_VALUE
                },
                to = if (from.toDate.isPresent && from.toTime.isPresent) {
                    timeFromDateTime(from.toDate.get(), from.toTime.get())
                } else {
                    Long.MAX_VALUE
                }
            )
            is FilterUiState.MinLostTime -> RadarProfile.Filter.MinLostTime(from.minLostTime.get().toMilliseconds())
            is FilterUiState.AppleAirdropContact -> RadarProfile.Filter.AppleAirdropContact(
                contactStr = from.contactString.trim(),
                airdropShaFormat = SHA256.fromStringAirdrop(from.contactString),
                minLostTime = from.minLostTime.orNull()?.toMilliseconds()
            )
            is FilterUiState.IsFollowing -> RadarProfile.Filter.IsFollowing(
                followingDurationMs = from.followingDurationMs.toMilliseconds(),
                followingDetectionIntervalMs = from.followingDetectionIntervalMs.toMilliseconds(),
            )
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
                this.manufacturer =
                    BluetoothSIG.bluetoothSIG[from.manufacturerId]?.let {
                        Optional.of(
                            ManufacturerInfo(
                                from.manufacturerId,
                                it,
                                null,
                            )
                        )
                    }
                        ?: Optional.empty()
            }
            is RadarProfile.Filter.IsFavorite -> FilterUiState.IsFavorite().apply {
                this.favorite = from.favorite
            }
            is RadarProfile.Filter.FirstDetectionInterval -> FilterUiState.FirstDetectionInterval().apply {
                this.fromDate =
                    if (from.from != Long.MIN_VALUE) Optional.of(from.from.toLocalDate()) else Optional.empty()
                this.fromTime =
                    if (from.from != Long.MIN_VALUE) Optional.of(from.from.toLocalTime()) else Optional.empty()
                this.toDate =
                    if (from.to != Long.MAX_VALUE) Optional.of(from.to.toLocalDate()) else Optional.empty()
                this.toTime =
                    if (from.to != Long.MAX_VALUE) Optional.of(from.to.toLocalTime()) else Optional.empty()
            }
            is RadarProfile.Filter.LastDetectionInterval -> FilterUiState.LastDetectionInterval().apply {
                this.fromDate =
                    if (from.from != Long.MIN_VALUE) Optional.of(from.from.toLocalDate()) else Optional.empty()
                this.fromTime =
                    if (from.from != Long.MIN_VALUE) Optional.of(from.from.toLocalTime()) else Optional.empty()
                this.toDate =
                    if (from.to != Long.MAX_VALUE) Optional.of(from.to.toLocalDate()) else Optional.empty()
                this.toTime =
                    if (from.to != Long.MAX_VALUE) Optional.of(from.to.toLocalTime()) else Optional.empty()
            }
            is RadarProfile.Filter.MinLostTime -> FilterUiState.MinLostTime().apply {
                this.minLostTime = Optional.of(from.minLostTime.toLocalTime())
            }
            is RadarProfile.Filter.All -> FilterUiState.All().apply {
                this.filters = from.filters.map { mapToUi(it) }
            }
            is RadarProfile.Filter.Any -> FilterUiState.Any().apply {
                this.filters = from.filters.map { mapToUi(it) }
            }
            is RadarProfile.Filter.Not -> FilterUiState.Not().apply {
                this.filter = Optional.of(mapToUi(from.filter))
            }
            is RadarProfile.Filter.AppleAirdropContact -> FilterUiState.AppleAirdropContact().apply {
                this.contactString = from.contactStr
                this.minLostTime = Optional.ofNullable(from.minLostTime?.toLocalTime())
            }
            is RadarProfile.Filter.IsFollowing -> FilterUiState.IsFollowing().apply {
                this.followingDurationMs = from.followingDurationMs.toLocalTime()
                this.followingDetectionIntervalMs = from.followingDetectionIntervalMs.toLocalTime()
            }
        }
    }
}