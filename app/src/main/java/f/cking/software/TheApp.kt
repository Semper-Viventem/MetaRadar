package f.cking.software

import android.app.Application

class TheApp : Application() {

    val permissionHelper: PermissionHelper = PermissionHelper()

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: TheApp
    }
}