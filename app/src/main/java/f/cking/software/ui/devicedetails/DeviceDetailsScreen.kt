package f.cking.software.ui.devicedetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
import f.cking.software.dateTimeStringFormat
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.LocationModel
import f.cking.software.dpToPx
import f.cking.software.frameRate
import f.cking.software.ui.AsyncBatchProcessor
import f.cking.software.ui.tagdialog.TagDialog
import f.cking.software.utils.graphic.MapView
import f.cking.software.utils.graphic.RoundedBox
import f.cking.software.utils.graphic.SystemNavbarSpacer
import f.cking.software.utils.graphic.TagChip
import kotlinx.coroutines.isActive
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
object DeviceDetailsScreen {

    @Composable
    fun Screen(address: String, key: String) {
        val viewModel: DeviceDetailsViewModel = koinViewModel(key = key) { parametersOf(address) }

        Scaffold(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize(),
            topBar = {
                AppBar(viewModel = viewModel)
            },
            content = {
                Content(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(it),
                    viewModel = viewModel,
                )
            }
        )
    }

    @Composable
    private fun AppBar(viewModel: DeviceDetailsViewModel) {
        val deviceData = viewModel.deviceState
        TopAppBar(
            title = {
                Text(text = stringResource(R.string.device_details_title))
            },
            actions = {
                if (deviceData != null) {
                    IconButton(onClick = { viewModel.onFavoriteClick(deviceData) }) {
                        val iconId =
                            if (deviceData.favorite) R.drawable.ic_star else R.drawable.ic_star_outline
                        val text = if (deviceData.favorite) stringResource(R.string.is_favorite) else stringResource(R.string.is_not_favorite)
                        Icon(
                            imageVector = ImageVector.vectorResource(id = iconId),
                            contentDescription = text,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.back() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }

    @Composable
    private fun Content(
        modifier: Modifier,
        viewModel: DeviceDetailsViewModel,
    ) {
        val deviceData = viewModel.deviceState
        if (deviceData == null) {
            Progress(modifier)
        } else {
            DeviceDetails(modifier = modifier, viewModel = viewModel, deviceData = deviceData)
        }
    }

    @Composable
    private fun Progress(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }

    @Composable
    private fun DeviceDetails(
        modifier: Modifier,
        viewModel: DeviceDetailsViewModel,
        deviceData: DeviceData
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            LocationHistory(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                deviceData = deviceData,
                viewModel = viewModel
            )
            Spacer(modifier = Modifier.height(16.dp))
            Tags(deviceData = deviceData, viewModel = viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            DeviceContent(modifier = Modifier.weight(1f), deviceData = deviceData)
            Spacer(modifier = Modifier.height(16.dp))
            SystemNavbarSpacer()
        }
    }

    @Composable
    private fun DeviceContent(
        modifier: Modifier = Modifier,
        deviceData: DeviceData,
    ) {

        RoundedBox(
            modifier = modifier
                .fillMaxWidth(),
            internalPaddings = 0.dp,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = deviceData.buildDisplayName(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = stringResource(R.string.device_details_name), fontWeight = FontWeight.Bold)
                Text(text = deviceData.name ?: stringResource(R.string.not_applicable))
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = stringResource(R.string.device_details_address), fontWeight = FontWeight.Bold)
                Text(text = deviceData.address)
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = stringResource(R.string.device_details_manufacturer), fontWeight = FontWeight.Bold)
                Text(text = deviceData.manufacturerInfo?.name ?: stringResource(R.string.not_applicable))
                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Text(
                        text = stringResource(R.string.device_details_detect_count),
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = deviceData.detectCount.toString())
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = stringResource(R.string.device_details_first_detection), fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(R.string.time_ago, deviceData.firstDetectionPeriod(LocalContext.current))
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = stringResource(R.string.device_details_last_detection), fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(R.string.time_ago, deviceData.lastDetectionPeriod(LocalContext.current))
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    @Composable
    fun Tags(
        deviceData: DeviceData,
        viewModel: DeviceDetailsViewModel,
    ) {
        RoundedBox(
            modifier = Modifier.fillMaxWidth(),
            internalPaddings = 0.dp
        ) {
            FlowRow(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
            ) {
                AddTag(viewModel = viewModel, deviceData = deviceData)
                deviceData.tags.forEach { tag ->
                    Tag(name = tag, viewModel = viewModel, deviceData = deviceData)
                }
            }
        }
    }

    @Composable
    fun Tag(
        deviceData: DeviceData,
        name: String,
        viewModel: DeviceDetailsViewModel,
    ) {
        val dialogState = rememberMaterialDialogState()

        MaterialDialog(
            dialogState = dialogState,
            buttons = {
                negativeButton(
                    text = stringResource(R.string.cancel),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.secondaryContainer)
                ) { dialogState.hide() }
                positiveButton(text = stringResource(R.string.confirm), textStyle = TextStyle(color = MaterialTheme.colorScheme.secondaryContainer)) {
                    dialogState.hide()
                    viewModel.onRemoveTagClick(deviceData, name)
                }
            },
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.delete_tag_title, name), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        TagChip(tagName = name, tagIcon = Icons.Filled.Delete) { dialogState.show() }
    }

    @Composable
    fun AddTag(
        deviceData: DeviceData,
        viewModel: DeviceDetailsViewModel,
    ) {
        val addTagDialog = TagDialog.rememberDialog {
            viewModel.onNewTagSelected(deviceData, it)
        }
        SuggestionChip(
            onClick = { addTagDialog.show() },
            icon = {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            },
            label = { Text(text = stringResource(R.string.add_tag)) }
        )
    }

    @Composable
    private fun HistoryPeriod(
        deviceData: DeviceData,
        viewModel: DeviceDetailsViewModel,
    ) {
        val dialog = rememberMaterialDialogState()
        MaterialDialog(
            dialogState = dialog,
            buttons = {
                negativeButton(
                    stringResource(R.string.cancel),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.secondaryContainer)
                ) { dialog.hide() }
            },
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(stringResource(R.string.change_history_period_dialog), fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                DeviceDetailsViewModel.HistoryPeriod.values().forEach { period ->
                    val isSelected = viewModel.historyPeriod == period
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.selectHistoryPeriodSelected(period, deviceData.address, autotunePeriod = false)
                            dialog.hide()
                        },
                        enabled = !isSelected,
                    ) {
                        val periodDisplayName = stringResource(period.displayNameRes)
                        val text = if (isSelected) {
                            stringResource(R.string.device_details_dialog_time_period_selected, periodDisplayName)
                        } else {
                            periodDisplayName
                        }
                        Text(text = text)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { dialog.show() },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text(text = stringResource(R.string.device_details_history_period), fontSize = 18.sp)
                        Text(text = stringResource(viewModel.historyPeriod.displayNameRes), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.device_details_history_period_subtitle),
                        fontWeight = FontWeight.Light,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Edit,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    contentDescription = stringResource(R.string.change)
                )
            }
        }
    }

    @Composable
    private fun LocationHistory(modifier: Modifier = Modifier, deviceData: DeviceData, viewModel: DeviceDetailsViewModel) {
        RoundedBox(modifier = modifier, internalPaddings = 0.dp) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                Map(
                    Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    isLoading = { viewModel.markersInLoadingState = it }
                )
                MapOverlay(viewModel = viewModel)
            }
            HistoryPeriod(deviceData = deviceData, viewModel = viewModel)
        }
    }

    @Composable
    private fun MapOverlay(
        viewModel: DeviceDetailsViewModel
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            if (viewModel.pointsState.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(modifier = Modifier.background(color = colorResource(id = R.color.black_30), shape = RoundedCornerShape(8.dp))) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = stringResource(R.string.device_details_no_location_history_for_such_period),
                            color = Color.White,
                        )
                    }
                }
            }

            if (viewModel.markersInLoadingState) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = Color.Black
                )
            }
        }
    }

    @Composable
    private fun Map(
        modifier: Modifier,
        viewModel: DeviceDetailsViewModel,
        isLoading: (isLoading: Boolean) -> Unit,
    ) {

        val scope = rememberCoroutineScope()
        val frameRate = LocalContext.current.frameRate()

        val batchProcessor = remember {
            AsyncBatchProcessor<LocationModel, MapView>(
                frameRate = frameRate,
                provideIsCancelled = { !scope.isActive },
                onBatchCompleted = { batchId, map ->
                    if (batchId % 10 == 0) {
                        map.invalidate()
                    }
                },
                processItem = { location, map ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(location.lat, location.lng)
                        title = location.time.dateTimeStringFormat("dd.MM.yy HH:mm")
                    }
                    map.overlays.add(marker)
                },
                onStart = { map ->
                    isLoading.invoke(true)
                    map.overlays.clear()
                    map.invalidate()
                },
                onComplete = { map ->
                    isLoading.invoke(false)
                    map.invalidate()
                },
                onCancelled = { map ->
                    isLoading.invoke(false)
                    map?.invalidate()
                }
            )
        }

        MapView(
            modifier = modifier,
            onLoad = { map -> initMapState(map) },
            onUpdate = { map -> refreshMap(map, viewModel, batchProcessor) }
        )
    }

    private fun initMapState(map: MapView) {
        map.setMultiTouchControls(true)
        map.minZoomLevel = MapConfig.MIN_MAP_ZOOM
        map.maxZoomLevel = MapConfig.MAX_MAP_ZOOM
        map.controller.setZoom(MapConfig.MIN_MAP_ZOOM)
    }

    private fun refreshMap(
        map: MapView,
        viewModel: DeviceDetailsViewModel,
        batchProcessor: AsyncBatchProcessor<LocationModel, MapView>,
    ) {

        val points = viewModel.pointsState
        batchProcessor.process(points, map)

        when (val cameraConfig = viewModel.cameraState) {
            is DeviceDetailsViewModel.MapCameraState.SinglePoint -> {
                Timber.d(cameraConfig.toString())
                val point = GeoPoint(cameraConfig.location.lat, cameraConfig.location.lng)
                map.controller.animateTo(
                    point,
                    cameraConfig.zoom,
                    if (cameraConfig.withAnimation) MapConfig.MAP_ANIMATION else MapConfig.MAP_NO_ANIMATION
                )
                map.invalidate()
            }

            is DeviceDetailsViewModel.MapCameraState.MultiplePoints -> {
                Timber.d(cameraConfig.toString())
                map.post {
                    map.zoomToBoundingBox(
                        BoundingBox.fromGeoPoints(cameraConfig.points.map { GeoPoint(it.lat, it.lng) }),
                        cameraConfig.withAnimation,
                        map.context.dpToPx(16f),
                        MapConfig.MAX_MAP_ZOOM,
                        MapConfig.MAP_ANIMATION,
                    )
                }
                map.invalidate()
            }
        }
    }
}