package f.cking.software.ui

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import f.cking.software.R
import f.cking.software.data.helpers.ActivityProvider
import f.cking.software.data.helpers.IntentHelper
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.isDarkModeOn
import f.cking.software.utils.navigation.BackCommand
import f.cking.software.utils.navigation.Navigator
import f.cking.software.utils.navigation.RouterImpl
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

        val navigationBarStyle = if (isDarkModeOn()) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(navigationBarStyle = navigationBarStyle)

        Configuration.getInstance().load(this, sharedPreferences)

        router.attachNavigator(viewModel.navigator)

        activityProvider.setActivity(this)

        setContent {
            val focusManager = LocalFocusManager.current
            MaterialTheme(
                colors = MaterialTheme.colors.copy(
                    primary = colorResource(id = R.color.primary),
                    primaryVariant = colorResource(id = R.color.primary_variant),
                    onPrimary = colorResource(id = R.color.on_primary),
                    secondary = colorResource(id = R.color.secondary),
                    secondaryVariant = colorResource(id = R.color.secondary_variant),
                    onSecondary = colorResource(id = R.color.on_secondary),
                    surface = colorResource(id = R.color.surface_color),
                    onSurface = colorResource(id = R.color.on_surface),
                ),
                typography = Typography(
                    body1 = MaterialTheme.typography.body1.copy(color = colorResource(id = R.color.on_surface)),
                    body2 = MaterialTheme.typography.body2.copy(color = colorResource(id = R.color.on_surface)),
                )
            ) {
                val stack = viewModel.navigator.stack
                if (stack.isEmpty()) {
                    finish()
                } else {
                    focusManager.clearFocus(true)
                    Column {
                        Box(
                            modifier = Modifier
                                .windowInsetsTopHeight(WindowInsets.statusBars)
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.primary)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                        ) {
                            stack.forEach { screen ->
                                screen()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.onPermissionGranted(requestCode, permissions, grantResults)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        intentHelper.handleActivityResult(requestCode, resultCode, data)
    }

    @Deprecated("Deprecated in Java")
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
