package f.cking.software.ui

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
            val dynamicColorsAreSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val darkMode = isSystemInDarkTheme()
            val colors = when {
                dynamicColorsAreSupported && darkMode -> dynamicDarkColorScheme(this)
                dynamicColorsAreSupported && !darkMode -> dynamicLightColorScheme(this)
                darkMode -> darkColorScheme(
                    primary = colorResource(id = R.color.theme_dark_primary),
                    primaryContainer = colorResource(id = R.color.theme_dark_primary_variant),
                    onPrimary = colorResource(id = R.color.theme_dark_on_primary),
                    surface = colorResource(id = R.color.theme_dark_surface_color),
                    surfaceVariant = colorResource(id = R.color.theme_dark_primary_variant),
                    surfaceContainer = colorResource(id = R.color.theme_light_primary_surface),
                    onSurface = colorResource(id = R.color.theme_dark_on_surface),
                    secondary = colorResource(id = R.color.theme_dark_secondary),
                    secondaryContainer = colorResource(id = R.color.theme_dark_secondary_variant),
                    onSecondary = colorResource(id = R.color.theme_dark_on_secondary),
                    error = colorResource(id = R.color.theme_dark_error),
                    onError = colorResource(id = R.color.theme_dark_on_error),
                )
                !darkMode -> darkColorScheme(
                    primary = colorResource(id = R.color.theme_light_primary),
                    primaryContainer = colorResource(id = R.color.theme_light_primary_variant),
                    onPrimary = colorResource(id = R.color.theme_light_on_primary),
                    surface = colorResource(id = R.color.theme_light_surface_color),
                    surfaceVariant = colorResource(id = R.color.theme_light_primary_variant),
                    surfaceContainer = colorResource(id = R.color.theme_dark_primary_surface),
                    onSurface = colorResource(id = R.color.theme_light_on_surface),
                    secondary = colorResource(id = R.color.theme_light_secondary),
                    secondaryContainer = colorResource(id = R.color.theme_light_secondary_variant),
                    onSecondary = colorResource(id = R.color.theme_light_on_secondary),
                    error = colorResource(id = R.color.theme_light_error),
                    onError = colorResource(id = R.color.theme_light_on_error),
                )
                else -> throw IllegalStateException("This state is unreachable")
            }
            MaterialTheme(
                colorScheme = colors,
                typography = Typography(
                    bodyMedium = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                    bodyLarge = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                    bodySmall = MaterialTheme.typography.bodySmall.copy(color = colors.onSurface),
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
