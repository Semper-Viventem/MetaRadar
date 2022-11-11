package f.cking.software

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.Room
import f.cking.software.data.AppDatabase
import f.cking.software.domain.helpers.BleScannerHelper
import f.cking.software.domain.helpers.PermissionHelper
import f.cking.software.domain.repo.DevicesRepository
import f.cking.software.domain.repo.SettingsRepository

class TheApp : Application() {

    lateinit var database: AppDatabase
    lateinit var permissionHelper: PermissionHelper
    lateinit var devicesRepository: DevicesRepository
    lateinit var bleScannerHelper: BleScannerHelper
    lateinit var settingsRepository: SettingsRepository
    var backgroundScannerIsActive by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        instance = this
        initSingletons()
    }

    private fun initSingletons() {
        database = Room.databaseBuilder(this, AppDatabase::class.java, DATABASE_NAME).build()
        settingsRepository = SettingsRepository(getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE))
        devicesRepository = DevicesRepository(database.deviceDao(), settingsRepository)
        bleScannerHelper = BleScannerHelper(devicesRepository, this)
        permissionHelper = PermissionHelper(this)
    }

    companion object {
        lateinit var instance: TheApp

        private const val SHARED_PREF_NAME = "app-prefs"
        private const val DATABASE_NAME = "app-database"
    }
}