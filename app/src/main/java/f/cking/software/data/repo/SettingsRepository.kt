package f.cking.software.data.repo

import android.content.SharedPreferences
import f.cking.software.TheAppConfig

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

    fun setGarbagingTime(time: Long) {
        sharedPreferences.edit().putLong(KEY_GARBAGING_TIME, time).apply()
    }

    fun getGarbagingTime(): Long {
        return sharedPreferences.getLong(KEY_GARBAGING_TIME, TheAppConfig.DEVICE_GARBAGING_TIME)
    }

    fun setUseGpsLocationOnly(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_USE_GPS_ONLY, value).apply()
    }

    fun getUseGpsLocationOnly(): Boolean {
        return sharedPreferences.getBoolean(KEY_USE_GPS_ONLY, TheAppConfig.USE_GPS_LOCATION_ONLY)
    }

    companion object {
        private const val KEY_SCAN_INTERVAL = "key_scan_interval"
        private const val KEY_SCAN_RESTRICTED_INTERVAL = "key_scan_restricted_interval"
        private const val KEY_SCAN_DURATION = "key_scan_duration"
        private const val KEY_GARBAGING_TIME = "key_garbaging_time"
        private const val KEY_USE_GPS_ONLY = "key_use_gps_location_only"
    }
}