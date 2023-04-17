package f.cking.software.domain.interactor.filterchecker

import android.util.Log
import f.cking.software.data.helpers.PowerModeHelper
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.RadarProfile

abstract class FilterChecker<T : RadarProfile.Filter>(
    private val powerModeHelper: PowerModeHelper,
) {

    private val cache: MutableMap<String, CacheValue> = mutableMapOf()

    suspend fun check(deviceData: DeviceData, filter: T): Boolean {
        val key = "${deviceData.address}_${filter.hashCode()}_${filter::class.simpleName}"
        val cacheValue = cache[key]
        val expirationTime = powerModeHelper.powerMode().filterCacheExpirationTime
        if (useCache() && cacheValue != null && System.currentTimeMillis() - cacheValue.time < expirationTime) {
            Log.d("FilterChecker", "Cache hit for $key")
            return cacheValue.value
        }
        val result = checkInternal(deviceData, filter)
        cache[key] = CacheValue(System.currentTimeMillis(), result)
        return result
    }

    abstract suspend fun checkInternal(deviceData: DeviceData, filter: T): Boolean

    protected open fun useCache(): Boolean = true

    open fun clearCache() {
        cache.clear()
    }

    private data class CacheValue(
        val time: Long,
        val value: Boolean,
    )
}