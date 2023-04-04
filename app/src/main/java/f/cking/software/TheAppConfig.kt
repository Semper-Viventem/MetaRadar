package f.cking.software

object TheAppConfig {
    const val DEFAULT_KNOWN_DEVICE_PERIOD_MS = 1000L * 60L * 60L // 1 hour
    const val DEVICE_GARBAGING_TIME = 1000L * 60L * 60L * 12L // 12 hours
    const val MIN_FOLLOWING_DURATION_TIME_MS = 15L * 60L * 1000L // 15 min
    const val MIN_FOLLOWING_INTERVAL_TIME_MS = 60L * 60L * 1000L // 1 hour
    const val USE_GPS_LOCATION_ONLY = false
}