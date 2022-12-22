package f.cking.software.domain.interactor.filterchecker

import f.cking.software.data.repo.DevicesRepository
import f.cking.software.domain.interactor.CheckDeviceIsFollowingInteractor
import f.cking.software.domain.model.AppleAirDrop
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.RadarProfile

class FilterCheckerImpl(
    private val checkDeviceIsFollowing: CheckDeviceIsFollowingInteractor,
    private val devicesRepository: DevicesRepository,
) : FilterChecker<RadarProfile.Filter> {

    private val lastDetectionInterval = FilterChecker<RadarProfile.Filter.LastDetectionInterval> { device, filter ->
        device.lastDetectTimeMs in filter.from..filter.to
    }
    private val firstDetectionInterval = FilterChecker<RadarProfile.Filter.FirstDetectionInterval> { device, filter ->
        device.firstDetectTimeMs in filter.from..filter.to
    }
    private val name = FilterChecker<RadarProfile.Filter.Name> { device, filter ->
        device.name != null && device.name.contains(filter.name, filter.ignoreCase)
    }
    private val address = FilterChecker<RadarProfile.Filter.Address> { device, filter ->
        device.address == filter.address
    }
    private val manufacturer = FilterChecker<RadarProfile.Filter.Manufacturer> { device, filter ->
        device.manufacturerInfo?.id?.let { it == filter.manufacturerId } ?: false
    }
    private val isFavorite = FilterChecker<RadarProfile.Filter.IsFavorite> { device, filter ->
        device.favorite == filter.favorite
    }
    private val minLostTime = FilterChecker<RadarProfile.Filter.MinLostTime> { device, filter ->
        System.currentTimeMillis() - device.lastDetectTimeMs >= filter.minLostTime
    }
    private val airdrop = FilterChecker<RadarProfile.Filter.AppleAirdropContact> { device, filter ->

        fun checkMinLostTime(contact: AppleAirDrop.AppleContact): Boolean {
            val currentTime = System.currentTimeMillis()
            return filter.minLostTime == null
                    || (contact.firstDetectionTimeMs == contact.lastDetectionTimeMs)
                    || (currentTime - contact.lastDetectionTimeMs >= filter.minLostTime)
        }

        device.manufacturerInfo?.airdrop?.contacts?.any { contact ->
            contact.sha256 == filter.airdropShaFormat && checkMinLostTime(contact)
        } == true
    }
    private val any = FilterChecker<RadarProfile.Filter.Any> { device, filter ->
        filter.filters.any { check(device, it) }
    }
    private val all = FilterChecker<RadarProfile.Filter.All> { device, filter ->
        filter.filters.all { check(device, it) }
    }
    private val not = FilterChecker<RadarProfile.Filter.Not> { device, filter ->
        !check(device, filter.filter)
    }
    private val isFollowing = FilterChecker<RadarProfile.Filter.IsFollowing> { deviceData, filter ->
        checkDeviceIsFollowing.execute(deviceData, filter.followingDurationMs, filter.followingDetectionIntervalMs)
            .apply {
                if (this) {
                    devicesRepository.saveFollowingDetection(deviceData, System.currentTimeMillis())
                }
            }
    }

    override suspend fun check(deviceData: DeviceData, filter: RadarProfile.Filter): Boolean {
        return when (filter) {
            is RadarProfile.Filter.LastDetectionInterval -> lastDetectionInterval.check(deviceData, filter)
            is RadarProfile.Filter.FirstDetectionInterval -> firstDetectionInterval.check(deviceData, filter)
            is RadarProfile.Filter.Name -> name.check(deviceData, filter)
            is RadarProfile.Filter.Address -> address.check(deviceData, filter)
            is RadarProfile.Filter.Manufacturer -> manufacturer.check(deviceData, filter)
            is RadarProfile.Filter.IsFavorite -> isFavorite.check(deviceData, filter)
            is RadarProfile.Filter.MinLostTime -> minLostTime.check(deviceData, filter)
            is RadarProfile.Filter.AppleAirdropContact -> airdrop.check(deviceData, filter)
            is RadarProfile.Filter.Any -> any.check(deviceData, filter)
            is RadarProfile.Filter.All -> all.check(deviceData, filter)
            is RadarProfile.Filter.Not -> not.check(deviceData, filter)
            is RadarProfile.Filter.IsFollowing -> isFollowing.check(deviceData, filter)
        }
    }

    private fun <T : RadarProfile.Filter> FilterChecker(
        check: suspend (deviceData: DeviceData, filter: T) -> Boolean
    ): FilterChecker<T> {

        return object : FilterChecker<T> {
            override suspend fun check(deviceData: DeviceData, filter: T): Boolean {
                return check.invoke(deviceData, filter)
            }
        }
    }
}