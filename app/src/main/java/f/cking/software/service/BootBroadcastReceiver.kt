package f.cking.software.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.data.repo.SettingsRepository
import f.cking.software.domain.interactor.SaveReportInteractor
import f.cking.software.domain.model.JournalEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class BootBroadcastReceiver : BroadcastReceiver() {

    private val permissionHelper: PermissionHelper by inject(PermissionHelper::class.java)
    private val settingsRepository: SettingsRepository by inject(SettingsRepository::class.java)
    private val saveReportInteractor: SaveReportInteractor by inject(SaveReportInteractor::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            tryToRunService(context)
        }
    }

    private fun tryToRunService(context: Context) {
        if (settingsRepository.getRunOnStartup()) {
            if (permissionHelper.checkAllPermissions()) {
                try {
                    BgScanService.start(context)
                } catch (error: Exception) {
                    Timber.e(error, "Failed to start service from the boot receiver")
                    val report = JournalEntry.Report.Error(
                        title = "[Launch on system startup error]: ${error.message ?: error::class.java}",
                        stackTrace = error.stackTraceToString(),
                    )
                    scope.launch {
                        saveReportInteractor.execute(report)
                    }
                }
            } else {
                Timber.e("Not all permissions granted, can't start service from the boot receiver")
            }
        }
    }
}