package f.cking.software.ui.main

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import f.cking.software.TheApp
import f.cking.software.domain.helpers.PermissionHelper

class MainActivity : AppCompatActivity() {

    private val TAG = "Main Activity"

    private val mainViewModel: MainViewModel by viewModels { MainViewModel.factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TheApp.instance.permissionHelper.setActivity(this)

        setContent {
            MainScreen.Screen(mainViewModel)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_CODE && allPermissionsGranted) {
            mainViewModel.runBackgroundScanning()
        }
    }

    override fun onDestroy() {
        TheApp.instance.permissionHelper.setActivity(null)
        super.onDestroy()
    }

}
