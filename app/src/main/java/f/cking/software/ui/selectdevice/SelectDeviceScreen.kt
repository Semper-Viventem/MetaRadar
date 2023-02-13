package f.cking.software.ui.selectdevice

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import f.cking.software.common.DeviceListItem
import f.cking.software.domain.model.DeviceData
import org.koin.androidx.compose.koinViewModel

object SelectDeviceScreen {

    private val generalComparator = Comparator<DeviceData> { second, first ->
        when {
            first.lastDetectTimeMs != second.lastDetectTimeMs -> first.lastDetectTimeMs.compareTo(second.lastDetectTimeMs)
            first.name != second.name -> first.name?.compareTo(second.name ?: return@Comparator 1) ?: -1
            first.manufacturerInfo?.name != second.manufacturerInfo?.name ->
                first.manufacturerInfo?.name?.compareTo(second.manufacturerInfo?.name ?: return@Comparator 1) ?: -1
            else -> first.address.compareTo(second.address)
        }
    }

    @Composable
    fun Screen(
        onSelected: (deviceData: DeviceData) -> Unit
    ) {
        val viewModel: SelectDeviceViewModel = koinViewModel()
        Scaffold(
            topBar = { AppBar(viewModel) },
            content = { paddings ->
                LazyColumn(modifier = Modifier.padding(paddings)) {
                    val list = viewModel.devices.asSequence()
                        .filter { device ->
                            viewModel.searchStr.takeIf { it.isNotBlank() }?.let { searchStr ->
                                (device.name?.contains(searchStr, true) ?: false)
                                        || (device.customName?.contains(searchStr, true) ?: false)
                                        || (device.manufacturerInfo?.name?.contains(searchStr, true) ?: false)
                                        || device.address.contains(searchStr, true)
                            } ?: true
                        }
                        .sortedWith(generalComparator)
                        .toList()

                    list.forEachIndexed { index, device ->
                        item {
                            DeviceListItem(device = device) {
                                onSelected.invoke(device)
                                viewModel.back()
                            }
                        }
                        val showDivider = list.getOrNull(index + 1)?.lastDetectTimeMs != device.lastDetectTimeMs
                        if (showDivider) {
                            item { Divider() }
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun AppBar(viewModel: SelectDeviceViewModel) {
        TopAppBar(
            title = {
                TextField(
                    value = viewModel.searchStr,
                    onValueChange = { viewModel.searchStr = it },
                    placeholder = { Text(text = "Search device", color = Color.White) }
                )
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.back() }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }
}