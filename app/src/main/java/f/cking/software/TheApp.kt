package f.cking.software

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.Room
import f.cking.software.data.AppDatabase
import f.cking.software.domain.BleScannerHelper
import f.cking.software.domain.DevicesRepository
import f.cking.software.domain.PermissionHelper
import java.util.*

class TheApp : Application() {

    lateinit var database: AppDatabase
    lateinit var permissionHelper: PermissionHelper
    lateinit var devicesRepository: DevicesRepository
    lateinit var bleScannerHelper: BleScannerHelper
    var activeWorkId by mutableStateOf<Optional<UUID>>(Optional.empty())

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