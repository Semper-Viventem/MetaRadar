package f.cking.software.ui

import android.annotation.SuppressLint
import android.app.ComponentCaller
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import f.cking.software.R
import f.cking.software.data.database.AppDatabase
import f.cking.software.data.helpers.ActivityProvider
import f.cking.software.data.helpers.IntentHelper
import f.cking.software.data.helpers.PermissionHelper
import f.cking.software.isDarkModeOn
import f.cking.software.utils.ScreenSizeLocal
import f.cking.software.utils.graphic.rememberProgressDialog
import f.cking.software.utils.navigation.BackCommand
import f.cking.software.utils.navigation.Navigator
import f.cking.software.utils.navigation.RouterImpl
import org.koin.android.ext.android.inject
import org.koin.compose.KoinContext
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
            val colors = themeColorScheme()


            KoinContext {
                MaterialTheme(
                    colorScheme = colors,
                    typography = Typography(
                        bodyMedium = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                        bodyLarge = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                        bodySmall = MaterialTheme.typography.bodySmall.copy(color = colors.onSurface),
                    )
                ) {
                    var screenSize by remember { mutableStateOf(IntSize(0, 0)) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { layoutCoordinates ->
                                screenSize = layoutCoordinates.size
                            }
                    ) {
                        val stack = viewModel.navigator.stack
                        if (stack.isEmpty()) {
                            finish()
                        } else {
                            focusManager.clearFocus(true)
                            CompositionLocalProvider(ScreenSizeLocal provides screenSize) {
                                stack.forEach { screen ->
                                    screen()
                                }
                            }
                        }
                    }

                    val preparingDatabaseDialog = rememberProgressDialog(stringResource(R.string.preparing_database))
                    val loadingDatabase by AppDatabase.loadDatabase.collectAsState(false)
                    if (loadingDatabase) {
                        preparingDatabaseDialog.show()
                    } else {
                        preparingDatabaseDialog.hide()
                    }
                }
            }
        }

        intentHelper.tryHandleIntent(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.onPermissionResult(requestCode, permissions, grantResults)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        intentHelper.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        intentHelper.tryHandleIntent(intent)
    }

    @SuppressLint("MissingSuperCall")
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

    @Composable
    private fun themeColorScheme(): ColorScheme {
        val dynamicColorsAreSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val darkMode = isSystemInDarkTheme()
        val colors = when {
            dynamicColorsAreSupported && darkMode -> dynamicDarkColorScheme(this)
            dynamicColorsAreSupported && !darkMode -> dynamicLightColorScheme(this)
            darkMode -> darkColorScheme(
                primary = colorResource(id = R.color.md_theme_dark_primary),
                onPrimary = colorResource(id = R.color.md_theme_dark_onPrimary),
                primaryContainer = colorResource(id = R.color.md_theme_dark_primaryContainer),
                onPrimaryContainer = colorResource(id = R.color.md_theme_dark_onPrimaryContainer),
                secondary = colorResource(id = R.color.md_theme_dark_secondary),
                onSecondary = colorResource(id = R.color.md_theme_dark_onSecondary),
                secondaryContainer = colorResource(id = R.color.md_theme_dark_secondaryContainer),
                onSecondaryContainer = colorResource(id = R.color.md_theme_dark_onSecondaryContainer),
                tertiary = colorResource(id = R.color.md_theme_dark_tertiary),
                onTertiary = colorResource(id = R.color.md_theme_dark_onTertiary),
                tertiaryContainer = colorResource(id = R.color.md_theme_dark_tertiaryContainer),
                onTertiaryContainer = colorResource(id = R.color.md_theme_dark_onTertiaryContainer),
                error = colorResource(id = R.color.md_theme_dark_error),
                errorContainer = colorResource(id = R.color.md_theme_dark_errorContainer),
                onError = colorResource(id = R.color.md_theme_dark_onError),
                onErrorContainer = colorResource(id = R.color.md_theme_dark_onErrorContainer),
                background = colorResource(id = R.color.md_theme_dark_background),
                onBackground = colorResource(id = R.color.md_theme_dark_onBackground),
                surface = colorResource(id = R.color.md_theme_dark_surface),
                surfaceContainer = colorResource(id = R.color.md_theme_dark_surfaceContainer),
                surfaceContainerHigh = colorResource(id = R.color.md_theme_dark_surfaceContainerHigh),
                surfaceContainerHighest = colorResource(id = R.color.md_theme_dark_surfaceContainerHighest),
                onSurface = colorResource(id = R.color.md_theme_dark_onSurface),
                surfaceVariant = colorResource(id = R.color.md_theme_dark_surfaceVariant),
                onSurfaceVariant = colorResource(id = R.color.md_theme_dark_onSurfaceVariant),
                outline = colorResource(id = R.color.md_theme_dark_outline),
                inverseOnSurface = colorResource(id = R.color.md_theme_dark_inverseOnSurface),
                inverseSurface = colorResource(id = R.color.md_theme_dark_inverseSurface),
                inversePrimary = colorResource(id = R.color.md_theme_dark_inversePrimary),
                surfaceTint = colorResource(id = R.color.md_theme_dark_surfaceTint),
                outlineVariant = colorResource(id = R.color.md_theme_dark_outlineVariant),
                scrim = colorResource(id = R.color.md_theme_dark_scrim),
            )

            !darkMode -> lightColorScheme(
                primary = colorResource(id = R.color.md_theme_light_primary),
                onPrimary = colorResource(id = R.color.md_theme_light_onPrimary),
                primaryContainer = colorResource(id = R.color.md_theme_light_primaryContainer),
                onPrimaryContainer = colorResource(id = R.color.md_theme_light_onPrimaryContainer),
                secondary = colorResource(id = R.color.md_theme_light_secondary),
                onSecondary = colorResource(id = R.color.md_theme_light_onSecondary),
                secondaryContainer = colorResource(id = R.color.md_theme_light_secondaryContainer),
                onSecondaryContainer = colorResource(id = R.color.md_theme_light_onSecondaryContainer),
                tertiary = colorResource(id = R.color.md_theme_light_tertiary),
                onTertiary = colorResource(id = R.color.md_theme_light_onTertiary),
                tertiaryContainer = colorResource(id = R.color.md_theme_light_tertiaryContainer),
                onTertiaryContainer = colorResource(id = R.color.md_theme_light_onTertiaryContainer),
                error = colorResource(id = R.color.md_theme_light_error),
                errorContainer = colorResource(id = R.color.md_theme_light_errorContainer),
                onError = colorResource(id = R.color.md_theme_light_onError),
                onErrorContainer = colorResource(id = R.color.md_theme_light_onErrorContainer),
                background = colorResource(id = R.color.md_theme_light_background),
                onBackground = colorResource(id = R.color.md_theme_light_onBackground),
                surface = colorResource(id = R.color.md_theme_light_surface),
                surfaceContainer = colorResource(id = R.color.md_theme_light_surfaceContainer),
                surfaceContainerHigh = colorResource(id = R.color.md_theme_light_surfaceContainerHigh),
                surfaceContainerHighest = colorResource(id = R.color.md_theme_light_surfaceContainerHighest),
                onSurface = colorResource(id = R.color.md_theme_light_onSurface),
                surfaceVariant = colorResource(id = R.color.md_theme_light_surfaceVariant),
                onSurfaceVariant = colorResource(id = R.color.md_theme_light_onSurfaceVariant),
                outline = colorResource(id = R.color.md_theme_light_outline),
                inverseOnSurface = colorResource(id = R.color.md_theme_light_inverseOnSurface),
                inverseSurface = colorResource(id = R.color.md_theme_light_inverseSurface),
                inversePrimary = colorResource(id = R.color.md_theme_light_inversePrimary),
                surfaceTint = colorResource(id = R.color.md_theme_light_surfaceTint),
                outlineVariant = colorResource(id = R.color.md_theme_light_outlineVariant),
                scrim = colorResource(id = R.color.md_theme_light_scrim),
            )

            else -> throw IllegalStateException("This state is unreachable")
        }
        return colors
    }
}
