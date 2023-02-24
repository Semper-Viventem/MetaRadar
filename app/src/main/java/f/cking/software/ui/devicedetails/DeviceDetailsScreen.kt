package f.cking.software.ui.devicedetails

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import f.cking.software.R
import f.cking.software.common.MapView
import f.cking.software.dateTimeStringFormat
import f.cking.software.domain.model.DeviceData
import f.cking.software.dpToPx
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

object DeviceDetailsScreen {

    @Composable
    fun Screen(address: String,key: String) {
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
                Text(text = "Device details")
            },
            actions = {
                if (deviceData != null) {
                    IconButton(onClick = { viewModel.onFavoriteClick(deviceData) }) {
                        val iconId =
                            if (deviceData.favorite) R.drawable.ic_star else R.drawable.ic_star_outline
                        val text = if (deviceData.favorite) "Is favorite" else "Is not favorite"
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
                        contentDescription = "Back",
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

            Text(text = "Name", fontWeight = FontWeight.Bold)
            Text(text = deviceData.name ?: "N/A")
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Address", fontWeight = FontWeight.Bold)
            Text(text = deviceData.address)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Manufacturer", fontWeight = FontWeight.Bold)
            Text(text = deviceData.manufacturerInfo?.name ?: "N/A")
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(text = "Detect count: ", fontWeight = FontWeight.Bold)
                Text(text = deviceData.detectCount.toString())
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "First detection", fontWeight = FontWeight.Bold)
            Text(text = deviceData.firstDetectionPeriod() + " ago")
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Last detection", fontWeight = FontWeight.Bold)
            Text(text = deviceData.lastDetectionPeriod() + " ago")
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    @Composable
    private fun HistoryPeriod(
        deviceData: DeviceData,
        viewModel: DeviceDetailsViewModel,
    ) {
        val expanded = remember { mutableStateOf(false) }

        if (expanded.value) {
            AlertDialog(
                onDismissRequest = { expanded.value = false },
                title = {
                    Text("Change location history period", fontSize = 20.sp, fontWeight = FontWeight.Black)
                },
                buttons = {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        DeviceDetailsViewModel.HistoryPeriod.values().forEach { period ->
                            val isSelected = viewModel.historyPeriod == period
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    viewModel.selectHistoryPeriodSelected(period, deviceData.address, autotunePeriod = false)
                                    expanded.value = false
                                },
                                enabled = !isSelected,
                            ) {
                                val text = if (isSelected) {
                                    "${period.displayName} (selected)"
                                } else {
                                    period.displayName
                                }
                                Text(text = text)
                            }
                        }
                    }
                },
            )
        }

        Box(
            modifier = Modifier
                .clickable { expanded.value = !expanded.value }
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
                        Text(text = "History period: ", fontSize = 18.sp)
                        Text(text = viewModel.historyPeriod.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Showing big location history may affect map performance",
                        fontWeight = FontWeight.Light
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Change"
                )
            }
        }
    }

    @Composable
    private fun Map(modifier: Modifier = Modifier, viewModel: DeviceDetailsViewModel) {
        val points = viewModel.points

        Box(
            modifier = modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {

            MapView(
                modifier = Modifier
                    .fillMaxWidth()
            ) { map ->
                map.setMultiTouchControls(true)
                map.overlays.clear()
                map.minZoomLevel = MIN_MAP_ZOOM
                map.maxZoomLevel = MAX_MAP_ZOOM

                if (points.isNotEmpty()) {
                    val markers = points.map { location ->
                        Marker(map).apply {
                            position = GeoPoint(location.lat, location.lng)
                            title = location.time.dateTimeStringFormat("dd.MM.yy HH:mm")
                        }
                    }
                    map.overlays.addAll(markers)
                    map.zoomToBoundingBox(
                        BoundingBox.fromGeoPoints(markers.map { it.position }),
                        !map.isCenter(),
                        map.context.dpToPx(16f),
                        MAX_MAP_ZOOM,
                        MAP_ANIMATION
                    )
                } else {
                    val location = viewModel.currentLocation
                    if (location != null) {
                        val animationSpeed = if (map.isCenter()) MAP_NO_ANIMATION else MAP_ANIMATION
                        map.controller.animateTo(GeoPoint(location.lat, location.lng), DEFAULT_MAP_ZOOM, animationSpeed)
                    }  else {
                        map.controller.setZoom(MIN_MAP_ZOOM)
                    }
                }
                map.invalidate()
            }

            if (points.isEmpty()) {
                Box(modifier = Modifier.background(color = colorResource(id = R.color.black_300), shape = RoundedCornerShape(8.dp))) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "No location history for such period",
                        color = Color.White,
                    )
                }
            }
        }

    }
    private fun MapView.isCenter(): Boolean = mapCenter.latitude == 0.0 && mapCenter.longitude == 0.0


    private const val MAP_ANIMATION = 300L
    private const val MAP_NO_ANIMATION = 0L
    private const val DEFAULT_MAP_ZOOM = 15.0
    private const val MAX_MAP_ZOOM = 18.0
    private const val MIN_MAP_ZOOM = 3.0
}