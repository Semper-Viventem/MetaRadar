package f.cking.software.ui.selectdevice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import f.cking.software.R
import f.cking.software.domain.model.DeviceData
import f.cking.software.utils.graphic.DeviceListItem
import f.cking.software.utils.graphic.GlassSystemNavbar
import f.cking.software.utils.graphic.SystemNavbarSpacer
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
object SelectDeviceScreen {

    @Composable
    fun Screen(
        onSelected: (deviceData: DeviceData) -> Unit
    ) {
        val viewModel: SelectDeviceViewModel = koinViewModel()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { AppBar(viewModel, scrollBehavior) },
            content = { paddings ->
                GlassSystemNavbar {
                    Content(Modifier.padding(paddings), viewModel, onSelected)
                }
            }
        )
    }

    @Composable
    private fun Content(
        modifier: Modifier,
        viewModel: SelectDeviceViewModel,
        onSelected: (deviceData: DeviceData) -> Unit,
    ) {
        LazyColumn(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize()
        ) {
            if (viewModel.loading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            val list = viewModel.devices
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
                item { SystemNavbarSpacer() }
            }
        }
    }

    @Composable
    private fun AppBar(viewModel: SelectDeviceViewModel, scrollBehavior: TopAppBarScrollBehavior) {
        TopAppBar(
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            title = {
                TextField(
                    value = viewModel.searchStr,
                    maxLines = 1,
                    onValueChange = { viewModel.searchRequest(it) },
                    placeholder = { Text(text = stringResource(R.string.search)) }
                )
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.back() }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )
    }
}