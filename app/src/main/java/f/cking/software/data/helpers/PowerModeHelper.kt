package f.cking.software.data.helpers

import android.content.Context
import android.os.PowerManager

class PowerModeHelper(
    private val context: Context,
) {

    private val powerManager by lazy { context.getSystemService(PowerManager::class.java) }
    private var cachedPowerMode: PowerMode = PowerMode.DEFAULT

    fun powerMode(useCached: Boolean = false): PowerMode {
        if (!useCached) {
            cachedPowerMode = when {
                powerManager.isPowerSaveMode -> PowerMode.POWER_SAVING
                !powerManager.isInteractive -> PowerMode.DEFAULT_RESTRICTED
                else -> PowerMode.DEFAULT
            }
        }

        return cachedPowerMode
    }

    enum class PowerMode(
        val scanDuration: Long,
        val scanInterval: Long,
        val useLocation: Boolean,
        val locationUpdateInterval: Long,
        val useRestrictedBleConfig: Boolean,
        val filterCacheExpirationTime: Long,
    ) {
        DEFAULT(
            scanDuration = 5_000L,
            scanInterval = 5_000L,
            useLocation = true,
            locationUpdateInterval = 10_000L,
            useRestrictedBleConfig = false,
            filterCacheExpirationTime = 3 * 60 * 1000L, // 3 minutes
        ),
        DEFAULT_RESTRICTED(
            scanDuration = 5_000L,
            scanInterval = 10_000L,
            useLocation = true,
            locationUpdateInterval = 10_000L,
            useRestrictedBleConfig = true,
            filterCacheExpirationTime = 5 * 60 * 1000L, // 5 minutes
        ),
        POWER_SAVING(
            scanDuration = 2_000L,
            scanInterval = 15_000L,
            useLocation = false,
            locationUpdateInterval = 60_000L,
            useRestrictedBleConfig = true,
            filterCacheExpirationTime = 10 * 60 * 1000L, // 10 minutes
        ),
    }
}