package f.cking.software.ui.devicelist

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import f.cking.software.common.DeviceListItem
import f.cking.software.common.Divider
import org.koin.androidx.compose.koinViewModel

object DeviceListScreen {

    @Composable
    fun Screen() {
        val viewModel: DeviceListViewModel = koinViewModel()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            val list = viewModel.devicesViewState
            list.mapIndexed { index, deviceData ->
                item { DeviceListItem(device = deviceData) { viewModel.onDeviceClick(deviceData) } }
                val showDivider = list.getOrNull(index + 1)?.lastDetectTimeMs != deviceData.lastDetectTimeMs
                if (showDivider) {
                    item { Divider() }
                }
            }
        }
    }
}