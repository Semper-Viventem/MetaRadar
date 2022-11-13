package f.cking.software.ui.devicelist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import f.cking.software.domain.model.DeviceData
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
                item { ListItem(listData = deviceData, viewModel) }
                val visibleDivider = list.getOrNull(index + 1)?.lastDetectTimeMs != deviceData.lastDetectTimeMs
                item { Divider(visible = visibleDivider) }
            }
        }
    }

    @Composable
    fun Divider(visible: Boolean) {
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

    @Composable
    fun ListItem(listData: DeviceData, viewModel: DeviceListViewModel) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.onDeviceClick(listData) }
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Row() {
                        Text(text = listData.name ?: "N/A", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    listData.manufacturerInfo?.name?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = it)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listData.address,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "lifetime: ${listData.firstDetectionPeriod()} | last update: ${listData.lastDetectionPeriod()} ago",
                        fontWeight = FontWeight.Light
                    )
                }
                if (listData.favorite) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Filled.Star, contentDescription = "Favorite")
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}