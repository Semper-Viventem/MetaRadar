package f.cking.software.ui.devicelist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import f.cking.software.common.DeviceListItem
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
            item { Divider(visible = false) }
            list.mapIndexed { index, deviceData ->
                item { DeviceListItem(device = deviceData) { viewModel.onDeviceClick(deviceData) } }
                val visibleDivider = list.getOrNull(index + 1)?.lastDetectTimeMs != deviceData.lastDetectTimeMs
                item { Divider(visible = visibleDivider) }
            }
        }
    }

    @Composable
    private fun Divider(visible: Boolean) {
        val color = if (visible) Color.LightGray else Color.Transparent
        val height = if (visible) 1.dp else 0.dp

        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .height(height)
                    .fillMaxWidth()
                    .background(color)
            )
        }
    }


}