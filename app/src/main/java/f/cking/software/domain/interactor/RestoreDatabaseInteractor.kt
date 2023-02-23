package f.cking.software.domain.interactor

import android.content.Context
import android.net.Uri
import com.jakewharton.processphoenix.ProcessPhoenix
import f.cking.software.TheApp
import f.cking.software.data.database.AppDatabase
import f.cking.software.data.helpers.IntentHelper
import f.cking.software.service.BgScanService

class RestoreDatabaseInteractor(
    private val appDatabase: AppDatabase,
    private val context: Context,
    private val intentHelper: IntentHelper,
) {

    suspend fun execute(uri: Uri) {
        BgScanService.stop(context)
        appDatabase.restoreDatabase(uri, context)
        TheApp.instance.restartKoin()
        ProcessPhoenix.triggerRebirth(context)
    }
}