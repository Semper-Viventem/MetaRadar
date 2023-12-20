package f.cking.software.ui.devicelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Button
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
import f.cking.software.ui.ScreenNavigationCommands
import f.cking.software.ui.filter.SelectFilterTypeScreen
import f.cking.software.utils.graphic.BottomSpacer
import f.cking.software.utils.graphic.ContentPlaceholder
import f.cking.software.utils.graphic.DeviceListItem
import f.cking.software.utils.graphic.Divider
import f.cking.software.utils.graphic.RoundedBox
import org.koin.androidx.compose.koinViewModel

object DeviceListScreen {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Screen() {
        val modifier = Modifier.background(MaterialTheme.colors.surface)
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
            ContentPlaceholder(stringResource(R.string.device_list_placeholder), modifier)
            if (viewModel.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colors.onPrimary
                )
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .nestedScroll(nestedScroll),
            ) {
                stickyHeader {
                    Box() {
                        Filters(viewModel)
                        if (viewModel.isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = MaterialTheme.colors.onPrimary
                            )
                        }
                    }
                }

                list.mapIndexed { index, deviceData ->
                    item {
                        DeviceListItem(
                            device = deviceData,
                            onClick = { viewModel.onDeviceClick(deviceData) },
                            onTagSelected = { viewModel.onTagSelected(it) },
                        )
                    }
                    val showDivider = list.getOrNull(index + 1)?.lastDetectTimeMs != deviceData.lastDetectTimeMs
                    if (showDivider) {
                        item { Divider() }
                    }
                }

                item {
                    BottomSpacer()
                }
            }
        }
    }

    @Composable
    private fun EnjoyTheApp(viewModel: DeviceListViewModel, enjoyTheAppState: DeviceListViewModel.EnjoyTheAppState) {
        RoundedBox {
            when (enjoyTheAppState) {
                DeviceListViewModel.EnjoyTheAppState.QUESTION -> EnjoyTheAppQuestion(viewModel)
                DeviceListViewModel.EnjoyTheAppState.LIKE -> EnjoyTheAppLike(viewModel)
                DeviceListViewModel.EnjoyTheAppState.DISLIKE -> EnjoyTheAppDislike(viewModel)
                DeviceListViewModel.EnjoyTheAppState.NONE -> throw IllegalStateException("EnjoyTheAppState.NONE is not supported here")
            }
        }
    }

    @Composable
    private fun EnjoyTheAppQuestion(viewModel: DeviceListViewModel) {
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.enjoy_the_app_question))
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.onEnjoyTheAppAnswered(true) }) {
                Text(text = stringResource(R.string.enjoy_the_app_yes))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.onEnjoyTheAppAnswered(true) }) {
                Text(text = stringResource(R.string.enjoy_the_app_yes))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    @Composable
    private fun EnjoyTheAppLike(viewModel: DeviceListViewModel) {
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.rate_the_app))
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = { viewModel.onEnjoyTheAppRatePlayStoreClick() }) {
                    Text(text = stringResource(R.string.rate_the_app_google_play))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.onEnjoyTheAppRateGithubClick() }) {
                    Text(text = stringResource(R.string.rate_the_app_github))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    @Composable
    private fun EnjoyTheAppDislike(viewModel: DeviceListViewModel) {
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.report_the_problem))
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.onEnjoyTheAppReportClick() }) {
                Text(text = stringResource(R.string.report))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun Filters(viewModel: DeviceListViewModel) {
        Surface(elevation = 4.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    val allFilters = (viewModel.quickFilters + viewModel.appliedFilter).toSet()

                    item { Spacer(modifier = Modifier.width(8.dp)) }

                    allFilters.forEach {
                        item {
                            val isSelected = viewModel.appliedFilter.contains(it)
                            val colors = if (isSelected) {
                                ChipDefaults.chipColors(
                                    backgroundColor = MaterialTheme.colors.primaryVariant,
                                    contentColor = MaterialTheme.colors.onPrimary,
                                    leadingIconContentColor = MaterialTheme.colors.onPrimary,
                                )
                            } else {
                                ChipDefaults.chipColors()
                            }

                            Chip(
                                colors = colors,
                                onClick = { viewModel.onFilterClick(it) },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            ) {
                                Text(text = it.displayName)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }

                    item {
                        SearchChip(viewModel)
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    item {
                        AddFilterChip(viewModel)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
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
        val colors = if (viewModel.isSearchMode) {
            ChipDefaults.chipColors(
                backgroundColor = MaterialTheme.colors.primaryVariant,
                contentColor = MaterialTheme.colors.onPrimary,
                leadingIconContentColor = MaterialTheme.colors.onPrimary,
            )
        } else {
            ChipDefaults.chipColors()
        }

        Chip(
            colors = colors,
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