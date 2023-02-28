package f.cking.software.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.data.repo.SettingsRepository
import org.koin.java.KoinJavaComponent.inject

class BootBroadcastReceiver : BroadcastReceiver() {

    private val permissionHelper: PermissionHelper by inject(PermissionHelper::class.java)
    private val settingsRepository: SettingsRepository by inject(SettingsRepository::class.java)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            tryToRunService(context)
        }
    }

    private fun tryToRunService(context: Context) {
        if (settingsRepository.getRunOnStartup() && permissionHelper.checkAllPermissions()) {
            BgScanService.start(context)
        }
    }
}