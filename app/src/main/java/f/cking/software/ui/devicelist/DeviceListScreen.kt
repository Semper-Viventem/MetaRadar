package f.cking.software.ui.devicelist

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.sp
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
import f.cking.software.ui.ScreenNavigationCommands
import f.cking.software.ui.filter.SelectFilterTypeScreen
import f.cking.software.utils.graphic.ContentPlaceholder
import f.cking.software.utils.graphic.DeviceListItem
import f.cking.software.utils.graphic.Divider
import f.cking.software.utils.graphic.FABSpacer
import f.cking.software.utils.graphic.RoundedBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
object DeviceListScreen {

    @Composable
    fun Screen() {
        val modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
        val viewModel: DeviceListViewModel = koinViewModel()
        val focusManager = LocalFocusManager.current

        val list = viewModel.devicesViewState
        if (list.isEmpty() && !viewModel.isSearchMode && viewModel.appliedFilter.isEmpty() && viewModel.currentBatchViewState == null) {
            ContentPlaceholder(stringResource(R.string.device_list_placeholder), modifier)
            if (viewModel.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val state = rememberLazyListState()
            val nestedScroll = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        focusManager.clearFocus(true)
                        if (list.isNotEmpty() && state.layoutInfo.visibleItemsInfo.any { it.contentType == ListContentType.PAGINATION_PROGRESS }) {
                            viewModel.onScrollEnd()
                        }
                        return super.onPreScroll(available, source)
                    }
                }
            }
            LazyColumn(
                modifier = modifier.nestedScroll(nestedScroll),
                state = state,
            ) {
                stickyHeader {
                    Box() {
                        Filters(viewModel)
                        if (viewModel.isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (viewModel.enjoyTheAppState != DeviceListViewModel.EnjoyTheAppState.None) {
                    item(contentType = ListContentType.ENJOY_THE_APP) {
                        Spacer(modifier = Modifier.height(8.dp))
                        EnjoyTheApp(viewModel, viewModel.enjoyTheAppState)
                    }
                }

                if (viewModel.currentBatchViewState != null) {
                    item(contentType = ListContentType.CURRENT_BATCH) {
                        CurrentBatch(viewModel)
                    }
                }

                list.mapIndexed { index, deviceData ->
                    item(contentType = ListContentType.DEVICE) {
                        DeviceListItem(
                            device = deviceData,
                            onClick = { viewModel.onDeviceClick(deviceData) },
                            onTagSelected = { viewModel.onTagSelected(it) },
                        )

                    }
                    val showDivider = list.getOrNull(index + 1)?.lastDetectTimeMs != deviceData.lastDetectTimeMs
                    if (showDivider) {
                        item(contentType = ListContentType.DIVIDER) { Divider() }
                    }
                }

                if (viewModel.isPaginationEnabled) {
                    item(contentType = ListContentType.PAGINATION_PROGRESS) {
                        Box(modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                item(contentType = ListContentType.BOTTOM_SPACER) {
                    FABSpacer()
                }
            }
        }
    }

    enum class ListContentType {
        ENJOY_THE_APP, CURRENT_BATCH, DEVICE, DIVIDER, PAGINATION_PROGRESS, BOTTOM_SPACER,
    }

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    @Composable
    fun CurrentBatch(
        viewModel: DeviceListViewModel,
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        RoundedBox(internalPaddings = 0.dp) {
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Spacer(modifier = Modifier.width(16.dp))
                var atEnd by remember { mutableStateOf(false) }
                val radarIcon = AnimatedImageVector.animatedVectorResource(id = R.drawable.radar_animation)
                val painter = rememberAnimatedVectorPainter(radarIcon, atEnd)
                val animatedPainter = rememberAnimatedVectorPainter(radarIcon, !atEnd)
                Image(
                    painter = if (atEnd) painter else animatedPainter,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.current_batch_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                )
                Spacer(modifier = Modifier.width(16.dp))
                LaunchedEffect(key1 = radarIcon) {
                    while (isActive) {
                        delay(1000)
                        atEnd = !atEnd
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            CurrentBatchList(viewModel)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    @Composable
    private fun CurrentBatchList(viewModel: DeviceListViewModel) {
        val currentBatch = viewModel.currentBatchViewState!!
        if (currentBatch.isNotEmpty()) {
            currentBatch.forEachIndexed { index, deviceData ->
                DeviceListItem(
                    device = deviceData,
                    showSignalData = true,
                    onClick = { viewModel.onDeviceClick(deviceData) },
                    onTagSelected = { viewModel.onTagSelected(it) },
                )
                if (index < currentBatch.lastIndex) {
                    Divider()
                }
            }
        } else {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.current_batch_empty),
                fontWeight = FontWeight.Light,
                fontSize = 16.sp,
            )
        }
    }

    @Composable
    private fun EnjoyTheApp(viewModel: DeviceListViewModel, enjoyTheAppState: DeviceListViewModel.EnjoyTheAppState) {
        RoundedBox {
            when (enjoyTheAppState) {
                is DeviceListViewModel.EnjoyTheAppState.Question -> EnjoyTheAppQuestion(viewModel)
                is DeviceListViewModel.EnjoyTheAppState.Like -> EnjoyTheAppLike(enjoyTheAppState, viewModel)
                is DeviceListViewModel.EnjoyTheAppState.Dislike -> EnjoyTheAppDislike(viewModel)
                is DeviceListViewModel.EnjoyTheAppState.None -> throw IllegalStateException("EnjoyTheAppState.NONE is not supported here")
            }
        }
    }

    @Composable
    private fun EnjoyTheAppQuestion(viewModel: DeviceListViewModel) {
        Column {
            Text(text = stringResource(R.string.enjoy_the_app_question), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(modifier = Modifier.weight(1f), onClick = { viewModel.onEnjoyTheAppAnswered(DeviceListViewModel.EnjoyTheAppAnswer.LIKE) }) {
                    Text(text = stringResource(R.string.enjoy_the_app_yes), color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(modifier = Modifier.weight(1f), onClick = { viewModel.onEnjoyTheAppAnswered(DeviceListViewModel.EnjoyTheAppAnswer.DISLIKE) }) {
                    Text(text = stringResource(R.string.enjoy_the_app_not_really), color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.onEnjoyTheAppAnswered(DeviceListViewModel.EnjoyTheAppAnswer.ASK_LATER) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(text = stringResource(R.string.enjoy_the_app_ask_later), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }

    @Composable
    private fun EnjoyTheAppLike(state: DeviceListViewModel.EnjoyTheAppState.Like, viewModel: DeviceListViewModel) {
        Column {
            Text(text = stringResource(R.string.rate_the_app), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                state.actions.forEachIndexed { i, action ->
                    Button(modifier = Modifier.weight(1f), onClick = { viewModel.onRateButtonClick(action) }) {
                        Text(text = action.title, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    if (i < state.actions.lastIndex) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun EnjoyTheAppDislike(viewModel: DeviceListViewModel) {
        Column {
            Text(text = stringResource(R.string.report_the_problem), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Button(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.onEnjoyTheAppReportClick() }) {
                Text(text = stringResource(R.string.report), color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    @Composable
    private fun Filters(viewModel: DeviceListViewModel) {
        Surface(shadowElevation = 4.dp) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    val allFilters = (viewModel.quickFilters + viewModel.appliedFilter).toSet()

                    item { Spacer(modifier = Modifier.width(16.dp)) }

                    item {
                        SearchChip(viewModel)
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    allFilters.forEach {
                        item {
                            val isSelected = viewModel.appliedFilter.contains(it)

                            FilterChip(
                                onClick = { viewModel.onFilterClick(it) },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                selected = isSelected,
                                label = {
                                    Text(text = it.displayName)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }

                    item {
                        AddFilterChip(viewModel)
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }

                if (viewModel.isSearchMode) {
                    SearchStr(viewModel)
                }
            }
        }
    }

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

        SuggestionChip(
            icon = {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(24.dp))
            },
            onClick = { selectFilterDialog.show() },
            label = {
                Text(text = stringResource(R.string.add_filter))
            }
        )
    }

    @Composable
    private fun SearchChip(viewModel: DeviceListViewModel) {
        FilterChip(
            leadingIcon = {
                val icon = if (viewModel.isSearchMode) Icons.Filled.Delete else Icons.Filled.Search
                Icon(icon, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(24.dp))
            },
            onClick = { viewModel.onOpenSearchClick() },
            selected = viewModel.isSearchMode,
            label = {
                Text(text = viewModel.searchQuery?.takeIf { it.isNotBlank() } ?: stringResource(R.string.search))
            }
        )
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