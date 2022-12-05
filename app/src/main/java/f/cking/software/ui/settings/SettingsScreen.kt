package f.cking.software.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
            ClearGarbageButton(viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            ClearLocationsButton(viewModel)
        }
    }

    @Composable
    private fun ClearGarbageButton(viewModel: SettingsViewModel) {
        Button(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            onClick = { viewModel.onRemoveGarbageClick() },
            enabled = !viewModel.garbageRemovingInProgress
        ) {
            Text(text = "Clear garbage")
        }
    }

    @Composable
    private fun ClearLocationsButton(viewModel: SettingsViewModel) {
        Button(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            onClick = { viewModel.onClearLocationsClick() },
            enabled = !viewModel.garbageRemovingInProgress
        ) {
            Text(text = "Clear locations history")
        }
    }
}