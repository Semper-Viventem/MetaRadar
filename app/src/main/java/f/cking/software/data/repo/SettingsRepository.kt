package f.cking.software.data.repo

import android.content.SharedPreferences
import f.cking.software.TheAppConfig
import f.cking.software.domain.model.RadarProfile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepository(
    private val sharedPreferences: SharedPreferences,
) {

    fun setScanInterval(scanInterval: Long) {
        sharedPreferences.edit().putLong(KEY_SCAN_INTERVAL, scanInterval).apply()
    }

    fun getScanInterval(): Long {
        return sharedPreferences.getLong(KEY_SCAN_INTERVAL, TheAppConfig.DEFAULT_SCAN_INTERVAL_MS)
    }

    fun setScanRestrictedInterval(scanInterval: Long) {
        sharedPreferences.edit().putLong(KEY_SCAN_RESTRICTED_INTERVAL, scanInterval).apply()
    }

    fun getScanRestrictedInterval(): Long {
        return sharedPreferences.getLong(KEY_SCAN_RESTRICTED_INTERVAL, TheAppConfig.RESTRICTED_MODE_SCAN_INTERVAL_MS)
    }

    fun setScanDuration(scanDuration: Long) {
        sharedPreferences.edit().putLong(KEY_SCAN_DURATION, scanDuration).apply()
    }

    fun getScanDuration(): Long {
        return sharedPreferences.getLong(KEY_SCAN_DURATION, TheAppConfig.DEFAULT_SCAN_DURATION_MS)
    }

    fun setKnownDevicePeriod(period: Long) {
        sharedPreferences.edit().putLong(KEY_KNOWN_PERIOD, period).apply()
    }

    fun getKnownDevicePeriod(): Long {
        return sharedPreferences.getLong(KEY_KNOWN_PERIOD, TheAppConfig.DEFAULT_KNOWN_DEVICE_PERIOD_MS)
    }

    fun setWantedDevicePeriod(period: Long) {
        sharedPreferences.edit().putLong(KEY_WANTED_PERIOD, period).apply()
    }

    fun getWantedDevicePeriod(): Long {
        return sharedPreferences.getLong(KEY_WANTED_PERIOD, TheAppConfig.DEFAULT_WANTED_PERIOD_MS)
    }

    fun setGarbagingTime(time: Long) {
        sharedPreferences.edit().putLong(KEY_GARBAGING_TIME, time).apply()
    }

    fun getGarbagingTime(): Long {
        return sharedPreferences.getLong(KEY_GARBAGING_TIME, TheAppConfig.DEVICE_GARBAGING_TIME)
    }

    fun setFollowingTurnedOn(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_FOLLOWING_DETECTION_TURNED_ON, value).apply()
    }

    fun isFollowingTurnedOn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_FOLLOWING_DETECTION_TURNED_ON, false)
    }

    fun getFollowingExcludingFilter(): RadarProfile.Filter? {
        val str = sharedPreferences.getString(KEY_EXCLUDE_FOLLOWING, null)
        return str?.let { Json.decodeFromString(it) }
    }

    fun setFollowingExcludingFilter(filter: RadarProfile.Filter?) {
        val str = filter?.let { Json.encodeToString(it) }
        sharedPreferences.edit().putString(KEY_EXCLUDE_FOLLOWING, str).apply()
    }

    companion object {
        private const val KEY_SCAN_INTERVAL = "key_scan_interval"
        private const val KEY_SCAN_RESTRICTED_INTERVAL = "key_scan_restricted_interval"
        private const val KEY_SCAN_DURATION = "key_scan_duration"
        private const val KEY_KNOWN_PERIOD = "key_known_period"
        private const val KEY_WANTED_PERIOD = "key_wanted_period"
        private const val KEY_GARBAGING_TIME = "key_garbaging_time"
        private const val KEY_EXCLUDE_FOLLOWING = "key_excluding_from_following_filter"
        private const val KEY_IS_FOLLOWING_DETECTION_TURNED_ON = "key_is_following_detection_turned_on"
    }
}