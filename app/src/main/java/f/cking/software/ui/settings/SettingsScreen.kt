package f.cking.software.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            Spacer(modifier = Modifier.height(16.dp))
            UseGpsLocationOnly(viewModel)
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