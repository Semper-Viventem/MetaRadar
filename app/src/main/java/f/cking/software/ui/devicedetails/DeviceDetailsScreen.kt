package f.cking.software.ui.devicedetails

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
import f.cking.software.common.MapView
import f.cking.software.dateTimeStringFormat
import f.cking.software.domain.model.DeviceData
import f.cking.software.domain.model.LocationModel
import f.cking.software.dpToPx
import f.cking.software.frameRate
import f.cking.software.ui.AsyncBatchProcessor
import kotlinx.coroutines.isActive
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

object DeviceDetailsScreen {

    private val TAG = "DeviceDetailsScreen"

    @Composable
    fun Screen(address: String, key: String) {
        val viewModel: DeviceDetailsViewModel = koinViewModel(key = key) { parametersOf(address) }

        Scaffold(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            topBar = {
                AppBar(viewModel = viewModel)
            },
            content = {
                Content(
                    modifier = Modifier.padding(it),
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
                            tint = Color.White
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.back() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White
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
            Progress()
        } else {
            DeviceDetails(modifier = modifier, viewModel = viewModel, deviceData = deviceData)
        }
    }

    @Composable
    private fun Progress(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(),
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
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            LocationHistory(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(), viewModel = viewModel
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item { DeviceContent(deviceData = deviceData, viewModel = viewModel) }
            }
        }
    }

    @Composable
    private fun DeviceContent(
        modifier: Modifier = Modifier,
        deviceData: DeviceData,
        viewModel: DeviceDetailsViewModel,
    ) {

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {


            HistoryPeriod(deviceData = deviceData, viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = deviceData.buildDisplayName(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
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
                Text(text = stringResource(R.string.device_details_detect_count), fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text(text = deviceData.detectCount.toString())
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = stringResource(R.string.device_details_first_detection), fontWeight = FontWeight.Bold)
            Text(text = stringResource(R.string.time_ago, deviceData.firstDetectionPeriod(LocalContext.current)))
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = stringResource(R.string.device_details_last_detection), fontWeight = FontWeight.Bold)
            Text(text = stringResource(R.string.time_ago, deviceData.lastDetectionPeriod(LocalContext.current)))
            Spacer(modifier = Modifier.height(8.dp))
        }
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
                negativeButton(stringResource(R.string.cancel)) { dialog.hide() }
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

        val shape = RoundedCornerShape(corner = CornerSize(8.dp))
        Box(
            modifier = Modifier
                .clip(shape = shape)
                .fillMaxWidth()
                .background(color = Color.LightGray, shape = shape,)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dialog.show() }
                    .padding(vertical = 8.dp, horizontal = 16.dp),
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
                        fontWeight = FontWeight.Light
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.change)
                )
            }
        }
    }

    @Composable
    private fun LocationHistory(modifier: Modifier = Modifier, viewModel: DeviceDetailsViewModel) {
        Box(modifier = modifier) {
            Map(
                viewModel = viewModel,
                isLoading = { viewModel.markersInLoadingState = it }
            )
            MapOverlay(viewModel = viewModel)
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
                    Box(modifier = Modifier.background(color = colorResource(id = R.color.black_300), shape = RoundedCornerShape(8.dp))) {
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
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
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
                Log.d(TAG, cameraConfig.toString())
                val point = GeoPoint(cameraConfig.location.lat, cameraConfig.location.lng)
                map.controller.animateTo(
                    point,
                    cameraConfig.zoom,
                    if (cameraConfig.withAnimation) MapConfig.MAP_ANIMATION else MapConfig.MAP_NO_ANIMATION
                )
                map.invalidate()
            }
            is DeviceDetailsViewModel.MapCameraState.MultiplePoints -> {
                Log.d(TAG, cameraConfig.toString())
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