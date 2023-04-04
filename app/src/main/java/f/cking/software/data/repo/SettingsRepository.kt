package f.cking.software.data.repo

import android.content.SharedPreferences
import f.cking.software.TheAppConfig

class SettingsRepository(
    private val sharedPreferences: SharedPreferences,
) {

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

    fun getPermissionsIntroWasShown(): Boolean {
        return sharedPreferences.getBoolean(KEY_PERMISSIONS_INTRO_WAS_SHOWN, false)
    }

    fun setPermissionsIntroWasShown(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_PERMISSIONS_INTRO_WAS_SHOWN, value).apply()
    }

    fun getRunOnStartup(): Boolean {
        return sharedPreferences.getBoolean(KEY_RUN_ON_STARTUP, false)
    }

    fun setRunOnStartup(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_RUN_ON_STARTUP, value).apply()
    }


    companion object {
        private const val KEY_GARBAGING_TIME = "key_garbaging_time"
        private const val KEY_USE_GPS_ONLY = "key_use_gps_location_only"
        private const val KEY_PERMISSIONS_INTRO_WAS_SHOWN = "key_permissions_intro_was_shown"
        private const val KEY_RUN_ON_STARTUP = "key_run_on_startup"
    }
}