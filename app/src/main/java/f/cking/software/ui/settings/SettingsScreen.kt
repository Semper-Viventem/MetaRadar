package f.cking.software.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                    Text(text = "No location data yet")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Location fetches only if the scanner is started", fontWeight = FontWeight.Light)
                } else {
                    Text(text = "Last location update time: ${locationData.emitTime.dateTimeStringFormat("HH:mm")}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "lat: ${locationData.location.latitude}", fontWeight = FontWeight.Light)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "lng: ${locationData.location.longitude}", fontWeight = FontWeight.Light)
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
            Text(text = "Clear garbage")
        }
    }

    @Composable
    private fun ClearLocationsButton(viewModel: SettingsViewModel) {
        val openDialog = remember { mutableStateOf(false) }
        if (openDialog.value) {
            AlertDialog(
                onDismissRequest = { openDialog.value = false },
                title = { Text(text = "Clear all location history?") },
                confirmButton = {
                    Button(onClick = {
                        openDialog.value = false
                        viewModel.onClearLocationsClick()
                    }) {
                        Text(text = "Confirm")
                    }
                },
                dismissButton = {
                    Button(onClick = { openDialog.value = false }) {
                        Text(text = "Cancel")
                    }
                },
            )
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onClick = { openDialog.value = true },
            enabled = !viewModel.locationRemovingInProgress
        ) {
            Text(text = "Clear locations history")
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
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Use GPS location only instead of fused location (Higher accuracy, higher battery consumption)"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = viewModel.useGpsLocationOnly,
                    onCheckedChange = { viewModel.onUseGpsLocationOnlyClick() }
                )
            }
        }
    }
}