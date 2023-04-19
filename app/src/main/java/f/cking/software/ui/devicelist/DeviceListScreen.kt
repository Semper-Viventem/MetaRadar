package f.cking.software.ui.devicelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
import f.cking.software.common.ContentPlaceholder
import f.cking.software.common.DeviceListItem
import f.cking.software.common.Divider
import f.cking.software.ui.ScreenNavigationCommands
import f.cking.software.ui.filter.SelectFilterTypeScreen
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

        val list = viewModel.devicesViewState

        if (list.isEmpty() && !viewModel.isSearchMode && viewModel.appliedFilter.isEmpty()) {
            ContentPlaceholder(stringResource(R.string.device_list_placeholder))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .nestedScroll(nestedScroll),
            ) {
                stickyHeader {
                    Filters(viewModel)
                }

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
                        val color = if (isSelected) MaterialTheme.colors.primarySurface else Color.LightGray
                        Chip(
                            colors = ChipDefaults.chipColors(
                                backgroundColor = color,
                                contentColor = Color.Black,
                                leadingIconContentColor = Color.Black,
                            ),
                            onClick = { viewModel.onFilterClick(it) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(24.dp))
                                }
                            }
                        ) {
                            Text(text = it.displayName)
                        }
                    }

                    SearchChip(viewModel)

                    AddFilterChip(viewModel)
                }

                if (viewModel.isSearchMode) {
                    SearchStr(viewModel)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun AddFilterChip(viewModel: DeviceListViewModel) {

        val filterName = stringResource(R.string.custom_filter)

        val selectFilterDialog = rememberMaterialDialogState()
        SelectFilterTypeScreen.Dialog(selectFilterDialog) { initialFilter ->
            viewModel.router.navigate(ScreenNavigationCommands.OpenCreateFilterScreen(
                initialFilterState = initialFilter,
            ) { filter ->
                val filterHolder = DeviceListViewModel.FilterHolder(
                    displayName = filterName,
                    filter = filter,
                )
                viewModel.onFilterClick(filterHolder)
            })
        }

        Chip(
            colors = ChipDefaults.chipColors(
                backgroundColor = Color.LightGray,
                contentColor = Color.Black,
                leadingIconContentColor = Color.Black,
            ),
            leadingIcon = {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(24.dp))
            },
            onClick = { selectFilterDialog.show() },
        ) {
            Text(text = stringResource(R.string.add_filter))
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun SearchChip(viewModel: DeviceListViewModel) {
        val color = if (viewModel.isSearchMode) MaterialTheme.colors.primarySurface else Color.LightGray

        Chip(
            colors = ChipDefaults.chipColors(
                backgroundColor = color,
                contentColor = Color.Black,
                leadingIconContentColor = Color.Black,
            ),
            leadingIcon = {
                val icon = if (viewModel.isSearchMode) Icons.Filled.Delete else Icons.Filled.Search
                Icon(icon, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(24.dp))
            },
            onClick = { viewModel.onOpenSearchClick() },
        ) {
            Text(text = viewModel.searchQuery?.takeIf { it.isNotBlank() } ?: stringResource(R.string.search))
        }
    }

    @Composable
    private fun SearchStr(viewModel: DeviceListViewModel) {
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
                placeholder = { Text(text = stringResource(R.string.search_query), fontWeight = FontWeight.Light) },
                trailingIcon = {
                    if (viewModel.searchQuery.isNullOrBlank()) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.close_search),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { viewModel.onOpenSearchClick() }
                        )
                    } else {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.clear_search),
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