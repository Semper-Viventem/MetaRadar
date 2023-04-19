package f.cking.software.domain.interactor.filterchecker

import f.cking.software.data.helpers.PowerModeHelper
import f.cking.software.data.repo.DevicesRepository
import f.cking.software.domain.interactor.CheckDeviceIsFollowingInteractor
import f.cking.software.domain.interactor.CheckDeviceLocationHistoryInteractor
import f.cking.software.domain.interactor.CheckUserLocationHistoryInteractor
import f.cking.software.domain.model.AppleAirDrop
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.RadarProfile

class FilterCheckerImpl(
    private val checkDeviceIsFollowing: CheckDeviceIsFollowingInteractor,
    private val devicesRepository: DevicesRepository,
    private val powerModeHelper: PowerModeHelper,
    private val checkDeviceLocationHistoryInteractor: CheckDeviceLocationHistoryInteractor,
    private val checkUserLocationHistoryInteractor: CheckUserLocationHistoryInteractor,
) : FilterChecker<RadarProfile.Filter>(powerModeHelper) {

    private val internalFilters: MutableList<FilterChecker<*>> = mutableListOf()

    private val lastDetectionInterval = filterChecker<RadarProfile.Filter.LastDetectionInterval> { device, filter ->
        device.lastDetectTimeMs in filter.from..filter.to
    }
    private val firstDetectionInterval = filterChecker<RadarProfile.Filter.FirstDetectionInterval>(useCache = true) { device, filter ->
        device.firstDetectTimeMs in filter.from..filter.to
    }
    private val name = filterChecker<RadarProfile.Filter.Name>(useCache = true) { device, filter ->
        device.name != null && device.name.contains(filter.name, filter.ignoreCase)
    }
    private val address = filterChecker<RadarProfile.Filter.Address>(useCache = true) { device, filter ->
        device.address == filter.address
    }
    private val manufacturer = filterChecker<RadarProfile.Filter.Manufacturer>(useCache = true) { device, filter ->
        device.manufacturerInfo?.id?.let { it == filter.manufacturerId } ?: false
    }
    private val isFavorite = filterChecker<RadarProfile.Filter.IsFavorite> { device, filter ->
        device.favorite == filter.favorite
    }
    private val minLostTime = filterChecker<RadarProfile.Filter.MinLostTime> { device, filter ->
        System.currentTimeMillis() - device.lastDetectTimeMs >= filter.minLostTime
    }
    private val airdrop = filterChecker<RadarProfile.Filter.AppleAirdropContact> { device, filter ->
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
    private val any = filterChecker<RadarProfile.Filter.Any> { device, filter ->
        filter.filters
            .sortedBy { it.difficulty }
            .any { check(device, it) }
    }
    private val all = filterChecker<RadarProfile.Filter.All> { device, filter ->
        filter.filters
            .sortedBy { it.difficulty }
            .all { check(device, it) }
    }
    private val not = filterChecker<RadarProfile.Filter.Not> { device, filter ->
        !check(device, filter.filter)
    }
    private val isFollowing = filterChecker<RadarProfile.Filter.IsFollowing> { deviceData, filter ->
        val detected = checkDeviceIsFollowing.execute(deviceData, filter.followingDurationMs, filter.followingDetectionIntervalMs)
        if (detected) {
            devicesRepository.saveFollowingDetection(deviceData, System.currentTimeMillis())
        }
        detected
    }
    private val deviceLocation = filterChecker<RadarProfile.Filter.DeviceLocation>(useCache = true) { device, filter ->
        checkDeviceLocationHistoryInteractor.execute(filter.location, filter.radiusMeters, device, filter.fromTimeMs, filter.toTimeMs)
    }
    private val userLocation = filterChecker<RadarProfile.Filter.UserLocation> { device, filter ->
        checkUserLocationHistoryInteractor.execute(filter.location, filter.radiusMeters, filter.noLocationDefaultValue)
    }

    override suspend fun checkInternal(deviceData: DeviceData, filter: RadarProfile.Filter): Boolean {
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
            is RadarProfile.Filter.DeviceLocation -> deviceLocation.check(deviceData, filter)
            is RadarProfile.Filter.UserLocation -> userLocation.check(deviceData, filter)
        }
    }

    override fun clearCache() {
        internalFilters.forEach { it.clearCache() }
    }

    override fun useCache(): Boolean {
        return false
    }

    private fun <T : RadarProfile.Filter> filterChecker(
        useCache: Boolean = false,
        check: suspend (deviceData: DeviceData, filter: T) -> Boolean,
    ): FilterChecker<T> {

        val filter = object : FilterChecker<T>(powerModeHelper) {
            override suspend fun checkInternal(deviceData: DeviceData, filter: T): Boolean {
                return check.invoke(deviceData, filter)
            }

            override fun useCache(): Boolean {
                return useCache
            }
        }

        internalFilters.add(filter)

        return filter
    }
}