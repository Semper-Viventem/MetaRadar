package f.cking.software.ui.main

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import f.cking.software.data.helpers.PermissionHelper
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val TAG = "Main Activity"

    private val mainViewModel: MainViewModel by viewModel()
    private val permissionHelper: PermissionHelper by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionHelper.setActivity(this)

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
        permissionHelper.setActivity(null)
        super.onDestroy()
    }

}
