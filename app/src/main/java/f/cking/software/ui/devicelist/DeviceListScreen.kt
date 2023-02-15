package f.cking.software.ui.devicelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import f.cking.software.common.DeviceListItem
import f.cking.software.common.Divider
import org.koin.androidx.compose.koinViewModel

object DeviceListScreen {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Screen() {
        val viewModel: DeviceListViewModel = koinViewModel()
        val focusManager = LocalFocusManager.current
        val nestedScroll = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    focusManager.clearFocus(true)
                    return super.onPreScroll(available, source)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .nestedScroll(nestedScroll),
        ) {
            stickyHeader {
                Filters(viewModel)
            }

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

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun Filters(viewModel: DeviceListViewModel) {
        Surface(elevation = 4.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                FlowRow(
                    modifier = Modifier.padding(8.dp),
                    mainAxisSpacing = 4.dp
                ) {
                    val allFilters = (viewModel.quickFilters + viewModel.appliedFilter).toSet()
                    allFilters.forEach {
                        val isSelected = viewModel.appliedFilter.contains(it)
                        val color = if (isSelected) {
                            MaterialTheme.colors.primarySurface
                        } else {
                            Color.LightGray
                        }
                        Chip(
                            colors = ChipDefaults.chipColors(
                                backgroundColor = color,
                                contentColor = Color.Black,
                                leadingIconContentColor = Color.Black,
                            ),
                            onClick = { viewModel.onFilterClick(it) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        ) {
                            Text(text = it.displayName)
                        }
                    }

                    val color = if (viewModel.isSearchMode) {
                        MaterialTheme.colors.primarySurface
                    } else {
                        Color.LightGray
                    }
                    Chip(
                        colors = ChipDefaults.chipColors(
                            backgroundColor = color,
                            contentColor = Color.Black,
                            leadingIconContentColor = Color.Black,
                        ),
                        leadingIcon = {
                            if (viewModel.isSearchMode) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        onClick = { viewModel.onOpenSearchClick() },
                    ) {
                        Text(text = viewModel.searchQuery?.takeIf { it.isNotBlank() } ?: "Search")
                    }
                }

                if (viewModel.isSearchMode) {
                    SearchStr(viewModel)
                }
            }
        }
    }

    @Composable
    fun SearchStr(viewModel: DeviceListViewModel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val focusRequest = remember { FocusRequester() }
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusTarget()
                    .focusRequester(focusRequest),
                value = viewModel.searchQuery.orEmpty(),
                onValueChange = { viewModel.onSearchInput(it) },
                placeholder = { Text(text = "Search query", fontWeight = FontWeight.Light) },
                trailingIcon = {
                    if (viewModel.searchQuery.isNullOrBlank()) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Close search",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { viewModel.onOpenSearchClick() }
                        )
                    } else {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear search",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { viewModel.onSearchInput("") }
                        )
                    }
                }
            )
            LaunchedEffect(Unit) {
                focusRequest.requestFocus()
            }
        }
    }
}