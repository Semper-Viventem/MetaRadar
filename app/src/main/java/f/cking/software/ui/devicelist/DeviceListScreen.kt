package f.cking.software.ui.devicelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            viewModel.devicesViewState.map { item { ListItem(listData = it, viewModel) } }
        }
    }

    @Composable
    fun ListItem(listData: DeviceData, viewModel: DeviceListViewModel) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { viewModel.onDeviceClick(listData) }
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row() {
                    Text(text = listData.name ?: "N/A", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (listData.favorite) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Filled.Star, contentDescription = "Favorite")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = listData.address + " " + listData.manufacturerInfo?.name.orEmpty())
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "lifetime: ${listData.firstDetectionPeriod()} | last update: ${listData.lastDetectionPeriod()} ago",
                    fontWeight = FontWeight.Light
                )
            }
            Text(text = listData.detectCount.toString(), modifier = Modifier.padding(8.dp))
        }
    }
}