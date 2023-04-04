package f.cking.software.data.helpers

import android.content.Context
import android.os.PowerManager

class PowerModeHelper(
    private val context: Context,
) {

    private val powerManager by lazy { context.getSystemService(PowerManager::class.java) }

    fun powerMode(): PowerMode {
        return when {
            powerManager.isPowerSaveMode -> PowerMode.POWER_SAVING
            !powerManager.isInteractive -> PowerMode.DEFAULT_RESTRICTED
            else -> PowerMode.DEFAULT
        }
    }

    enum class PowerMode(
        val scanDuration: Long,
        val scanInterval: Long,
        val useLocation: Boolean,
        val locationUpdateInterval: Long,
        val useRestrictedBleConfig: Boolean,
    ) {
        DEFAULT(
            scanDuration = 5_000L,
            scanInterval = 5_000L,
            useLocation = true,
            locationUpdateInterval = 10_000L,
            useRestrictedBleConfig = false,
        ),
        DEFAULT_RESTRICTED(
            scanDuration = 5_000L,
            scanInterval = 10_000L,
            useLocation = true,
            locationUpdateInterval = 10_000L,
            useRestrictedBleConfig = true,
        ),
        POWER_SAVING(
            scanDuration = 2_000L,
            scanInterval = 15_000L,
            useLocation = false,
            locationUpdateInterval = 60_000L,
            useRestrictedBleConfig = true,
        ),
    }
}