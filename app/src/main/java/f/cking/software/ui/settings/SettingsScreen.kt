package f.cking.software.ui.settings

import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.BuildConfig
import f.cking.software.R
import f.cking.software.dateTimeStringFormat
import f.cking.software.utils.graphic.BottomNavigationSpacer
import f.cking.software.utils.graphic.FABSpacer
import f.cking.software.utils.graphic.RoundedBox
import f.cking.software.utils.graphic.Switcher
import f.cking.software.utils.graphic.ThemedDialog
import org.koin.androidx.compose.koinViewModel

object SettingsScreen {

    @Composable
    fun Screen() {
        val viewModel: SettingsViewModel = koinViewModel()
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            ProjectInformationBlock(viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            AppSettings(viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            DatabaseBlock(viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            LocationBlock(viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            if (BuildConfig.DEBUG) {
                ShaderTestButton(viewModel = viewModel)
                Spacer(modifier = Modifier.height(8.dp))
            }
            AppInfo()
            SecretCatPhoto()
            FABSpacer()
        }
    }

    @Composable
    private fun LocationInfo(viewModel: SettingsViewModel) {
        Column {
            val locationData = viewModel.locationData
            if (locationData == null) {
                Text(text = stringResource(R.string.no_location_data_yet), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.location_fetches_only_if_service_is_turned_on), fontWeight = FontWeight.Light)
            } else {
                val formattedTime = locationData.emitTime.dateTimeStringFormat("HH:mm")
                Text(text = stringResource(R.string.last_location_update_time, formattedTime), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.lat_template, locationData.location.latitude), fontWeight = FontWeight.Light)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = stringResource(R.string.lng_template, locationData.location.longitude), fontWeight = FontWeight.Light)
            }
        }
    }

    @Composable
    private fun LocationBlock(viewModel: SettingsViewModel) {
        RoundedBox(internalPaddings = 0.dp) {
            Box(modifier = Modifier.padding(16.dp)) {
                LocationInfo(viewModel)
            }
            UseGpsLocationOnly(viewModel)
        }
    }

    @Composable
    private fun DatabaseBlock(viewModel: SettingsViewModel) {
        RoundedBox {
            Text(text = stringResource(R.string.database_information), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            val databaseInfo = viewModel.databaseInfo
            if (databaseInfo != null) {
                Text(text = stringResource(R.string.database_size, Formatter.formatFileSize(LocalContext.current, databaseInfo.sizeBytes)))
                Text(text = stringResource(R.string.database_devices_count, databaseInfo.totalDevices.toString()))
                Text(text = stringResource(R.string.database_locations_count, databaseInfo.totalGeotags.toString()))

                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(text = stringResource(R.string.database_actions), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            BackupDB(viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            RestoreDB(viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            ClearGarbageButton(viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            ClearLocationsButton(viewModel)
        }
    }

    @Composable
    private fun ClearGarbageButton(viewModel: SettingsViewModel) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.onRemoveGarbageClick() },
            enabled = !viewModel.garbageRemovingInProgress
        ) {
            Text(text = stringResource(R.string.clear_garbage), color = MaterialTheme.colorScheme.onPrimary)
        }
    }

    @Composable
    private fun RestoreDB(viewModel: SettingsViewModel) {
        val dialogState = rememberMaterialDialogState()

        ThemedDialog(
            dialogState = dialogState,
            buttons = {
                negativeButton(
                    text = stringResource(R.string.cancel),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ) { dialogState.hide() }
                positiveButton(text = stringResource(R.string.confirm), textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    dialogState.hide()
                    viewModel.onRestoreDBClick()
                }
            },
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.restore_data_from_file_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = stringResource(R.string.restore_data_from_file_subtitle))
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { dialogState.show() },
            enabled = !viewModel.backupDbInProgress
        ) {
            Text(text = stringResource(R.string.settings_restore_database), color = MaterialTheme.colorScheme.onPrimary)
        }
    }

    @Composable
    private fun BackupDB(viewModel: SettingsViewModel) {
        val dialogState = rememberMaterialDialogState()

        ThemedDialog(
            dialogState = dialogState,
            buttons = {
                negativeButton(
                    text = stringResource(R.string.cancel),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ) { dialogState.hide() }
                positiveButton(text = stringResource(R.string.confirm), textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    dialogState.hide()
                    viewModel.onBackupDBClick()
                }
            },
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.backup_database_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = stringResource(R.string.backup_database_subtitle))
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { dialogState.show() },
            enabled = !viewModel.backupDbInProgress
        ) {
            Text(text = stringResource(R.string.settings_backup_database), color = MaterialTheme.colorScheme.onPrimary)
        }
    }

    @Composable
    private fun SecretCatPhoto() {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            BottomNavigationSpacer()
            repeat(50) {
                Image(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .alpha(0.1f)
                        .width(30.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    painter = painterResource(id = R.drawable.cat_footprint),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Image(
                    painter = painterResource(id = R.drawable.appa),
                    contentDescription = stringResource(id = R.string.secret_cat),
                    contentScale = ContentScale.FillWidth
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(id = R.string.secret_cat), fontWeight = FontWeight.Light)
            }
        }
    }

    @Composable
    private fun ClearLocationsButton(viewModel: SettingsViewModel) {

        val dialogState = rememberMaterialDialogState()

        ThemedDialog(
            dialogState = dialogState,
            buttons = {
                negativeButton(
                    text = stringResource(R.string.cancel),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ) { dialogState.hide() }
                positiveButton(text = stringResource(R.string.confirm), textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    dialogState.hide()
                    viewModel.onClearLocationsClick()
                }
            },
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.clear_all_location_history_dialog_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { dialogState.show() },
            enabled = !viewModel.locationRemovingInProgress
        ) {
            Text(text = stringResource(R.string.settings_clear_all_location_history), color = MaterialTheme.colorScheme.onPrimary)
        }
    }

    @Composable
    private fun UseGpsLocationOnly(viewModel: SettingsViewModel) {
        Switcher(
            value = viewModel.useGpsLocationOnly,
            title = stringResource(R.string.settings_use_gps_title),
            subtitle = stringResource(R.string.settings_use_gps_subtitle),
            onClick = { viewModel.onUseGpsLocationOnlyClick() }
        )
    }

    @Composable
    private fun AppSettings(viewModel: SettingsViewModel) {
        RoundedBox(internalPaddings = 0.dp) {
            Text(modifier = Modifier.padding(16.dp), text = stringResource(id = R.string.app_settings_title), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Switcher(
                value = viewModel.silentModeEnabled,
                title = stringResource(R.string.silent_mode_title),
                subtitle = stringResource(id = R.string.silent_mode_subtitle),
                onClick = { viewModel.changeSilentMode() }
            )
            Switcher(
                value = viewModel.runOnStartup,
                title = stringResource(R.string.launch_on_system_startup_title),
                subtitle = null,
                onClick = { viewModel.setRunOnStartup() }
            )
            Switcher(
                value = viewModel.deepAnalysisEnabled,
                title = stringResource(R.string.enable_deep_analysis),
                subtitle = stringResource(R.string.enable_deep_analysis_description),
                onClick = { viewModel.onEnableDeepAnalysisClick() }
            )
            Switcher(
                value = viewModel.wakeUpWhileScanning,
                title = stringResource(R.string.settings_keep_screen_on_while_scanning_title),
                subtitle = stringResource(R.string.settings_keep_screen_on_while_scanning_description),
                onClick = { viewModel.toggleWakeUpOnScreen() }
            )
        }
    }

    @Composable
    private fun ProjectInformationBlock(viewModel: SettingsViewModel) {
        RoundedBox {
            Button(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.onProjectPurposeClick() }) {
                Text(text = stringResource(R.string.button_project_purpose), color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.project_github_title, stringResource(id = R.string.app_name)), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Button(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.onGithubClick() }) {
                Text(text = stringResource(R.string.open_github), color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.report_issue_title), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Button(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.onReportIssueClick() }) {
                Text(text = stringResource(R.string.report), color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    @Composable
    private fun AppInfo() {
        RoundedBox {
            Text(text = stringResource(R.string.app_info_title), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.app_info_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(if (BuildConfig.DEBUG) R.string.app_info_build_type_debug else R.string.app_info_build_type_release))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.app_info_distribution, BuildConfig.DISTRIBUTION))
        }
    }

    @Composable
    private fun ShaderTestButton(viewModel: SettingsViewModel) {
        RoundedBox {
            Button(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.openShadersTest() }) {
                Text(text = stringResource(R.string.shaders_test), color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}