package f.cking.software.domain.interactor.filterchecker

import android.util.LruCache
import f.cking.software.data.helpers.PowerModeHelper
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.RadarProfile

abstract class FilterChecker<T : RadarProfile.Filter>(
    private val powerModeHelper: PowerModeHelper,
) {

    private val cache: LruCache<String, CacheValue> = LruCache(MAX_CACHE_SIZE)

    suspend fun check(deviceData: DeviceData, filter: T): Boolean {
        val key = "${deviceData.address}_${filter.hashCode()}_${filter::class.simpleName}"
        val cacheValue = cache[key]
        if (useCache() && cacheValue != null
            && System.currentTimeMillis() - cacheValue.time < powerModeHelper.powerMode(useCached = true).filterCacheExpirationTime
        ) {
            // Timber.d("Cache hit for $key")
            return cacheValue.value
        }
        val result = checkInternal(deviceData, filter)
        cache.put(key, CacheValue(System.currentTimeMillis(), result))
        return result
    }

    abstract suspend fun checkInternal(deviceData: DeviceData, filter: T): Boolean

    protected open fun useCache(): Boolean = true

    open fun clearCache() {
        cache.evictAll()
    }

    private data class CacheValue(
        val time: Long,
        val value: Boolean,
    )

    companion object {
        private const val MAX_CACHE_SIZE = 5000
    }
}