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

    fun getFirstAppLaunchTime(): Long {
        return sharedPreferences.getLong(KEY_FIRST_APP_LAUNCH_TIME, NO_APP_LAUNCH_TIME)
    }

    fun setFirstAppLaunchTime(value: Long) {
        sharedPreferences.edit().putLong(KEY_FIRST_APP_LAUNCH_TIME, value).apply()
    }

    fun getEnjoyTheAppAnswered(): Boolean {
        return sharedPreferences.getBoolean(KEY_ENJOY_THE_APP_ANSWERED, false)
    }

    fun setEnjoyTheAppAnswered(value: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ENJOY_THE_APP_ANSWERED, value).apply()
    }

    fun getEnjoyTheAppStartingPoint(): Long {
        return sharedPreferences.getLong(KEY_ENJOY_THE_APP_STARTING_POINT, NO_ENJOY_THE_APP_STARTING_POINT)
    }

    fun setEnjoyTheAppStartingPoint(value: Long) {
        sharedPreferences.edit().putLong(KEY_ENJOY_THE_APP_STARTING_POINT, value).apply()
    }

    companion object {
        private const val KEY_GARBAGING_TIME = "key_garbaging_time"
        private const val KEY_USE_GPS_ONLY = "key_use_gps_location_only"
        private const val KEY_PERMISSIONS_INTRO_WAS_SHOWN = "key_permissions_intro_was_shown"
        private const val KEY_RUN_ON_STARTUP = "key_run_on_startup"
        private const val KEY_FIRST_APP_LAUNCH_TIME = "key_first_app_launch_time"
        private const val KEY_ENJOY_THE_APP_ANSWERED = "key_enjoy_the_app_answered_v1"
        private const val KEY_ENJOY_THE_APP_STARTING_POINT = "key_enjoy_the_app_starting_point"

        const val NO_APP_LAUNCH_TIME = -1L
        const val NO_ENJOY_THE_APP_STARTING_POINT = -1L
    }
}