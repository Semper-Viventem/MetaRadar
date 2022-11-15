package f.cking.software.ui.selectdevice

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import f.cking.software.common.DeviceListItem
import f.cking.software.domain.model.DeviceData
import org.koin.androidx.compose.koinViewModel

object SelectDeviceScreen {

    @Composable
    fun Screen(
        onSelected: (type: DeviceData) -> Unit
    ) {
        val viewModel: SelectDeviceViewModel = koinViewModel()
        Scaffold(
            topBar = { AppBar(viewModel) },
            content = { paddings ->
                LazyColumn(modifier = Modifier.padding(paddings)) {
                    viewModel.devices.forEach { type ->
                        item {
                            DeviceListItem(device = type) {
                                onSelected.invoke(type)
                                viewModel.back()
                            }
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
                Text(text = "Select device")
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.back() }) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }
}