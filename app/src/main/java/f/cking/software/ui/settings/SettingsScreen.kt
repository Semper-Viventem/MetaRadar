package f.cking.software.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.BuildConfig
import f.cking.software.R
import f.cking.software.dateTimeStringFormat
import f.cking.software.utils.graphic.BottomOffsetWithFAB
import f.cking.software.utils.graphic.RoundedBox
import org.koin.androidx.compose.koinViewModel

object SettingsScreen {

    @Composable
    fun Screen() {
        val viewModel: SettingsViewModel = koinViewModel()
        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ProjectGithub(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                    ReportIssue(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                    ClearDatabaseBlock(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                    BackupDatabaseBlock(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                    RunOnStartup(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                    LocationBlock(viewModel = viewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppInfo()
                    SecretCatPhoto()
                    BottomOffsetWithFAB()
                }
            }
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
    private fun ClearDatabaseBlock(viewModel: SettingsViewModel) {
        RoundedBox {
            ClearGarbageButton(viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            ClearLocationsButton(viewModel)
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
    private fun BackupDatabaseBlock(viewModel: SettingsViewModel) {
        RoundedBox {
            BackupDB(viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            RestoreDB(viewModel = viewModel)
        }
    }

    @Composable
    private fun ClearGarbageButton(viewModel: SettingsViewModel) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.onRemoveGarbageClick() },
            enabled = !viewModel.garbageRemovingInProgress
        ) {
            Text(text = stringResource(R.string.clear_garbage))
        }
    }

    @Composable
    private fun RestoreDB(viewModel: SettingsViewModel) {
        val dialogState = rememberMaterialDialogState()

        MaterialDialog(
            dialogState = dialogState,
            buttons = {
                negativeButton(
                    text = stringResource(R.string.cancel),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.secondaryContainer)
                ) { dialogState.hide() }
                positiveButton(text = stringResource(R.string.confirm), textStyle = TextStyle(color = MaterialTheme.colorScheme.secondaryContainer)) {
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
            Text(text = stringResource(R.string.settings_restore_database))
        }
    }

    @Composable
    private fun BackupDB(viewModel: SettingsViewModel) {
        val dialogState = rememberMaterialDialogState()

        MaterialDialog(
            dialogState = dialogState,
            buttons = {
                negativeButton(
                    text = stringResource(R.string.cancel),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.secondaryContainer)
                ) { dialogState.hide() }
                positiveButton(text = stringResource(R.string.confirm), textStyle = TextStyle(color = MaterialTheme.colorScheme.secondaryContainer)) {
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
            Text(text = stringResource(R.string.settings_backup_database))
        }
    }

    @Composable
    private fun SecretCatPhoto() {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            repeat(50) {
                Image(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .alpha(0.3f)
                        .width(30.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    painter = painterResource(id = R.drawable.cat_footprint),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(id = R.drawable.appa),
                contentDescription = stringResource(id = R.string.secret_cat),
                contentScale = ContentScale.FillWidth
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(id = R.string.secret_cat), fontWeight = FontWeight.Light)
        }
    }

    @Composable
    private fun ClearLocationsButton(viewModel: SettingsViewModel) {

        val dialogState = rememberMaterialDialogState()

        MaterialDialog(
            dialogState = dialogState,
            buttons = {
                negativeButton(
                    text = stringResource(R.string.cancel),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.secondaryContainer)
                ) { dialogState.hide() }
                positiveButton(text = stringResource(R.string.confirm), textStyle = TextStyle(color = MaterialTheme.colorScheme.secondaryContainer)) {
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
            Text(text = stringResource(R.string.settings_clear_all_location_history))
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
    private fun RunOnStartup(viewModel: SettingsViewModel) {
        RoundedBox(internalPaddings = 0.dp) {
            Switcher(
                value = viewModel.runOnStartup,
                title = stringResource(R.string.launch_on_system_startup_title),
                subtitle = null,
                onClick = { viewModel.setRunOnStartup() }
            )
        }
    }

    @Composable
    private fun ReportIssue(viewModel: SettingsViewModel) {
        RoundedBox {
            Text(text = stringResource(R.string.report_issue_title), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Button(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.opReportIssueClick() }) {
                Text(text = stringResource(R.string.report))
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
    private fun ProjectGithub(viewModel: SettingsViewModel) {
        RoundedBox {
            Text(text = stringResource(R.string.project_github_title, stringResource(id = R.string.app_name)), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Button(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.onGithubClick() }) {
                Text(text = stringResource(R.string.open_github))
            }
        }
    }

    @Composable
    private fun Switcher(
        value: Boolean,
        title: String,
        subtitle: String?,
        onClick: () -> Unit,
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick.invoke() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = title)
                    subtitle?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = it, fontWeight = FontWeight.Light, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = value,
                    onCheckedChange = { onClick.invoke() }
                )
            }
        }
    }
}