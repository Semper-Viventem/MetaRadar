package f.cking.software.domain.interactor

import android.net.Uri
import com.jakewharton.processphoenix.ProcessPhoenix
import f.cking.software.TheApp
import f.cking.software.data.database.AppDatabase
import f.cking.software.service.BgScanService

class RestoreDatabaseInteractor(
    private val appDatabase: AppDatabase,
    private val application: TheApp,
) {
    suspend fun execute(uri: Uri) {
        BgScanService.stop(application)
        appDatabase.restoreDatabase(uri, application)
        application.restartKoin()
        ProcessPhoenix.triggerRebirth(application)
    }
}
