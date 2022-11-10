package f.cking.software

import android.app.Application
import androidx.room.Room
import f.cking.software.data.AppDatabase
import f.cking.software.domain.BleScannerHelper
import f.cking.software.domain.DevicesRepository
import f.cking.software.domain.PermissionHelper

class TheApp : Application() {

    lateinit var database: AppDatabase
    lateinit var permissionHelper: PermissionHelper
    lateinit var devicesRepository: DevicesRepository
    lateinit var bleScannerHelper: BleScannerHelper

    override fun onCreate() {
        super.onCreate()
        instance = this
        initSingletons()
    }

    private fun initSingletons() {
        database = Room.databaseBuilder(this, AppDatabase::class.java, "app-database").build()
        devicesRepository = DevicesRepository(database.deviceDao())
        permissionHelper = PermissionHelper()
        bleScannerHelper = BleScannerHelper(this)
    }

    companion object {
        lateinit var instance: TheApp
    }
}