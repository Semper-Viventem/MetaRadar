package f.cking.software.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import f.cking.software.R
import f.cking.software.common.navigation.BackCommand
import f.cking.software.common.navigation.Navigator
import f.cking.software.common.navigation.RouterImpl
import f.cking.software.data.helpers.ActivityProvider
import f.cking.software.data.helpers.IntentHelper
import f.cking.software.data.helpers.PermissionHelper
import org.koin.android.ext.android.inject
import org.osmdroid.config.Configuration

class MainActivity : AppCompatActivity() {

    private val TAG = "Main Activity"

    private val permissionHelper: PermissionHelper by inject()
    private val intentHelper: IntentHelper by inject()
    private val activityProvider: ActivityProvider by inject()
    private val router: RouterImpl by inject()
    private val sharedPreferences: SharedPreferences by inject()
    private val viewModel: MainActivityViewModel by viewModels {
        viewModelFactory { initializer { MainActivityViewModel() } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, sharedPreferences)

        router.attachNavigator(viewModel.navigator)

        activityProvider.setActivity(this)

        setContent {
            val focusManager = LocalFocusManager.current
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
                val stack = viewModel.navigator.stack
                if (stack.isEmpty()) {
                    finish()
                } else {
                    focusManager.clearFocus(true)
                    stack.forEach { screen ->
                        screen()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.onPermissionGranted(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        intentHelper.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        router.navigate(BackCommand)
    }

    override fun onDestroy() {
        activityProvider.setActivity(null)
        router.detachNavigator()
        super.onDestroy()
    }

    private class MainActivityViewModel : ViewModel() {
        val navigator: Navigator = Navigator(root = ScreenNavigationCommands.OpenMainScreen)
    }
}
