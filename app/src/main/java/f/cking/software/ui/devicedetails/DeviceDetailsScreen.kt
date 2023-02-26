package f.cking.software.ui.devicedetails

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import f.cking.software.dpToPx
import f.cking.software.frameRate
import kotlinx.coroutines.*
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.*

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
            Map(modifier = Modifier.weight(1f), viewModel = viewModel)
            DeviceContent(deviceData = deviceData, viewModel = viewModel)
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

        Box(
            modifier = Modifier
                .clickable { dialog.show() }
                .fillMaxWidth()
                .background(
                    color = Color.LightGray,
                    shape = RoundedCornerShape(corner = CornerSize(8.dp)),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
    private fun Map(modifier: Modifier = Modifier, viewModel: DeviceDetailsViewModel) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val batchProcessor = remember {
            AsyncBatchProcessor<Marker, MapView>(
                framerate = context.frameRate(),
                provideIsCancelled = { !scope.isActive },
                processItem = { marker, map -> map.overlays.add(marker) },
            )
        }

        Box(
            modifier = modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {

            MapView(
                modifier = Modifier.fillMaxWidth(),
                onLoad = { map -> configureMap(map, viewModel, scope, batchProcessor) }
            )

            val points = viewModel.points
            if (points.isEmpty()) {
                Box(modifier = Modifier.background(color = colorResource(id = R.color.black_300), shape = RoundedCornerShape(8.dp))) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.device_details_no_location_history_for_such_period),
                        color = Color.White,
                    )
                }
            }
        }
    }

    private fun configureMap(
        map: MapView,
        viewModel: DeviceDetailsViewModel,
        scope: CoroutineScope,
        batchProcessor: AsyncBatchProcessor<Marker, MapView>
    ) {
        map.setMultiTouchControls(true)
        map.overlays.clear()
        map.minZoomLevel = MIN_MAP_ZOOM
        map.maxZoomLevel = MAX_MAP_ZOOM
        map.controller.setZoom(MIN_MAP_ZOOM)

        val points = viewModel.points

        val location = viewModel.currentLocation

        if (points.isNotEmpty()) {
            scope.launch {
                val markers = withContext(Dispatchers.Default) {
                    points.mapIndexed { i, location ->
                        Marker(map).apply {
                            position = GeoPoint(location.lat, location.lng)
                            title = location.time.dateTimeStringFormat("dd.MM.yy HH:mm")
                        }
                    }
                }

                batchProcessor.process(markers, map) {
                    map.zoomToBoundingBox(
                        BoundingBox.fromGeoPoints(markers.map { it.position }),
                        !map.isCenter(),
                        map.context.dpToPx(16f),
                        MAX_MAP_ZOOM,
                        MAP_ANIMATION
                    )
                }
            }
        } else {
            if (location != null) {
                val animationSpeed = if (map.isCenter()) MAP_NO_ANIMATION else MAP_ANIMATION
                map.controller.animateTo(GeoPoint(location.lat, location.lng), DEFAULT_MAP_ZOOM, animationSpeed)
            } else {
                map.controller.setZoom(MIN_MAP_ZOOM)
            }
        }
    }

    class AsyncBatchProcessor<T, P>(
        private val framerate: Float,
        private val provideIsCancelled: () -> Boolean,
        private val processItem: (item: T, payload: P) -> Unit,
    ) {

        private val handler = Handler(Looper.getMainLooper())
        private val activeTasks = mutableSetOf<String>()

        fun process(iterable: Iterable<T>, payload: P, onComplete: (payload: P) -> Unit) {
            cancel()
            val task = UUID.randomUUID().toString()
            activeTasks.add(task)
            processInternal(iterable.iterator(), task, payload, onComplete)
        }

        fun cancel() {
            handler.removeCallbacksAndMessages(TOKEN)
            activeTasks.clear()
        }

        private fun processInternal(
            iterator: Iterator<T>,
            taskKey: String,
            payload: P,
            onComplete: (payload: P) -> Unit,
        ) {

            val batchTimeout = (1000 / framerate / 2).toLong()
            val drawStartTime = SystemClock.elapsedRealtime()
            fun shouldDelayUntilNextFrame(drawStartTime: Long): Boolean = SystemClock.elapsedRealtime() - drawStartTime > batchTimeout

            var shouldWaitNextFrame = false
            while (iterator.hasNext() && !shouldWaitNextFrame && !provideIsCancelled.invoke() && activeTasks.contains(taskKey)) {
                val next = iterator.next()
                processItem.invoke(next, payload)
                shouldWaitNextFrame = shouldDelayUntilNextFrame(drawStartTime)
            }

            if (shouldWaitNextFrame) {
                handler.postDelayed({ processInternal(iterator, taskKey, payload, onComplete) }, TOKEN, framerate.toLong() / 2)
            } else if (!iterator.hasNext() && !provideIsCancelled.invoke() && activeTasks.contains(taskKey)) {
                onComplete.invoke(payload)
            }
        }

        companion object {
            private const val TOKEN = "batch_token"
        }
    }

    private fun MapView.isCenter(): Boolean = mapCenter.latitude == 0.0 && mapCenter.longitude == 0.0


    private const val MAP_ANIMATION = 300L
    private const val MAP_NO_ANIMATION = 0L
    private const val DEFAULT_MAP_ZOOM = 15.0
    private const val MAX_MAP_ZOOM = 18.0
    private const val MIN_MAP_ZOOM = 3.0
}