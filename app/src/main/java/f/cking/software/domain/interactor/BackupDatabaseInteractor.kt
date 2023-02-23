package f.cking.software.domain.interactor

import android.content.Context
import android.net.Uri
import f.cking.software.data.database.AppDatabase

class BackupDatabaseInteractor(
    private val appDatabase: AppDatabase,
    private val context: Context,
) {

    suspend fun execute(uri: Uri) {
        appDatabase.backupDatabase(uri, context)
    }
}