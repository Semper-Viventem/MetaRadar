package f.cking.software.ui

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import f.cking.software.R
import f.cking.software.common.navigation.BackCommand
import f.cking.software.common.navigation.NavRouter
import f.cking.software.common.navigation.Navigator
import f.cking.software.data.helpers.PermissionHelper
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val TAG = "Main Activity"

    private val permissionHelper: PermissionHelper by inject()
    private val router: NavRouter by inject()

    private val navigator = Navigator(root = ScreenNavigationCommands.OpenMainScreen)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        router.attachNavigator(navigator)

        permissionHelper.setActivity(this)

        setContent {
            MaterialTheme(
                colors = MaterialTheme.colors.copy(
                    primary = colorResource(id = R.color.orange_500),
                    primaryVariant = colorResource(id = R.color.orange_700),
                    onPrimary = Color.White,
                    secondary = Color.Black,
                    secondaryVariant = Color.Black,
                    onSecondary = Color.White,
                )
            ) {
                val stack = navigator.stack
                if (stack.isEmpty()) {
                    finish()
                } else {
                    stack.forEach { screen ->
                        screen()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (requestCode == PermissionHelper.PERMISSIONS_REQUEST_CODE && allPermissionsGranted) {
            permissionHelper.pendingPermissionGranted()
        }
    }

    override fun onBackPressed() {
        router.navigate(BackCommand)
    }

    override fun onDestroy() {
        permissionHelper.setActivity(null)
        router.detachNavigator()
        super.onDestroy()
    }

}
