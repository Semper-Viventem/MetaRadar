package f.cking.software.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
import f.cking.software.dateTimeStringFormat
import org.koin.androidx.compose.koinViewModel

object SettingsScreen {

    @Composable
    fun Screen() {
        val viewModel: SettingsViewModel = koinViewModel()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            ClearGarbageButton(viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            ClearLocationsButton(viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            LocationInfo(viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            UseGpsLocationOnly(viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            BackupDB(viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
            RestoreDB(viewModel = viewModel)
        }
    }

    @Composable
    private fun LocationInfo(viewModel: SettingsViewModel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.LightGray,
                        shape = RoundedCornerShape(corner = CornerSize(8.dp)),
                    )
                    .padding(8.dp)
            ) {
                val locationData = viewModel.locationData
                if (locationData == null) {
                    Text(text = stringResource(R.string.no_location_data_yet))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.location_fetches_only_if_service_is_turned_on),
                        fontWeight = FontWeight.Light
                    )
                } else {
                    val formattedTime = locationData.emitTime.dateTimeStringFormat("HH:mm")
                    Text(
                        text = stringResource(R.string.last_location_update_time, formattedTime)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.lat_template, locationData.location.latitude),
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.lng_template, locationData.location.longitude),
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }

    @Composable
    private fun ClearGarbageButton(viewModel: SettingsViewModel) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
                negativeButton(text = stringResource(R.string.cancel)) { dialogState.hide() }
                positiveButton(text = stringResource(R.string.confirm)) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
                negativeButton(text = stringResource(R.string.cancel)) { dialogState.hide() }
                positiveButton(text = stringResource(R.string.confirm)) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onClick = { dialogState.show() },
            enabled = !viewModel.backupDbInProgress
        ) {
            Text(text = stringResource(R.string.settings_backup_database))
        }
    }

    @Composable
    private fun ClearLocationsButton(viewModel: SettingsViewModel) {

        val dialogState = rememberMaterialDialogState()

        MaterialDialog(
            dialogState = dialogState,
            buttons = {
                negativeButton(text = stringResource(R.string.cancel)) { dialogState.hide() }
                positiveButton(text = stringResource(R.string.confirm)) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onClick = { dialogState.show() },
            enabled = !viewModel.locationRemovingInProgress
        ) {
            Text(text = stringResource(R.string.settings_clear_all_location_history))
        }
    }

    @Composable
    private fun UseGpsLocationOnly(viewModel: SettingsViewModel) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.onUseGpsLocationOnlyClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.settings_use_gps_title))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = stringResource(R.string.settings_use_gps_subtitle), fontWeight = FontWeight.Light, fontSize = 12.sp,)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = viewModel.useGpsLocationOnly,
                    onCheckedChange = { viewModel.onUseGpsLocationOnlyClick() }
                )
            }
        }
    }
}