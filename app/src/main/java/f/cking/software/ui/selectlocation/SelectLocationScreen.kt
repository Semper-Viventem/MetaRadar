package f.cking.software.ui.selectlocation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.AbsoluteCutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import f.cking.software.R
import f.cking.software.TheAppConfig
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.domain.model.LocationModel
import f.cking.software.ui.devicedetails.MapConfig
import f.cking.software.ui.map.MapView
import f.cking.software.utils.graphic.RoundedBox
import f.cking.software.utils.graphic.SystemNavbarSpacer
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getKoin
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@OptIn(ExperimentalMaterial3Api::class)
object SelectLocationScreen {

    @Composable
    fun Screen(
        initialLocationModel: LocationModel?,
        initialRadius: Float?,
        onSelected: (location: LocationModel, radiusMeters: Float) -> Unit,
        onCloseClick: () -> Unit,
    ) {
        Scaffold(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize(),
            topBar = { AppBar(onCloseClick) },
            content = { paddings ->
                Content(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(top = paddings.calculateTopPadding()),
                    onSelected = onSelected,
                    initialLocationModel = initialLocationModel,
                    initialRadius = initialRadius
                )
            }
        )
    }

    @Composable
    private fun AppBar(onCloseClick: () -> Unit) {
        TopAppBar(
            title = {
                Text(text = stringResource(R.string.select_location))
            },
            navigationIcon = {
                IconButton(onClick = onCloseClick) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )
    }

    @Composable
    private fun Content(
        modifier: Modifier = Modifier,
        initialLocationModel: LocationModel?,
        initialRadius: Float?,
        onSelected: (location: LocationModel, radiusMeters: Float) -> Unit,
    ) {
        val map = remember { mutableStateOf<MapView?>(null) }

        Column(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            RoundedBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                internalPaddings = 0.dp,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Map(
                        modifier = Modifier.fillMaxSize(),
                        initialLocationModel = initialLocationModel,
                        onMapReady = { map.value = it }
                    )

                    if (map.value != null) {
                        Box(
                            modifier = Modifier
                                .size(width = 20.dp, height = 10.dp)
                                .blur(2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 10.dp, height = 5.dp)
                                    .background(color = Color.DarkGray, shape = AbsoluteCutCornerShape(10.dp))
                            )
                        }
                        Column(
                            modifier = Modifier
                                .height(120.dp)
                                .width(60.dp)
                        ) {
                            val painter = painterResource(R.drawable.ic_location)
                            Image(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                contentScale = ContentScale.FillWidth,
                                painter = painter,
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
            BottomPanel(map.value, initialRadius, onSelected)
        }
    }

    @Composable
    private fun BottomPanel(
        map: MapView?,
        initialRadius: Float?,
        onSelected: (location: LocationModel, radiusMeters: Float) -> Unit,
    ) {
        val radiusMeters = remember { mutableStateOf(initialRadius ?: TheAppConfig.DEFAULT_LOCATION_FILTER_RADIUS) }

        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.select_location_radius, radiusMeters.value),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Slider(
                modifier = Modifier
                    .fillMaxWidth(),
                value = radiusMeters.value,
                onValueChange = { value -> radiusMeters.value = value },
                valueRange = 5f..1000f,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth(),
                enabled = map != null,
                onClick = {
                    val cameraCenter = map!!.mapCenter
                    onSelected.invoke(
                        LocationModel(cameraCenter.latitude, cameraCenter.longitude, System.currentTimeMillis()),
                        radiusMeters.value
                    )
                }
            ) {
                Text(text = stringResource(R.string.confirm), color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            SystemNavbarSpacer()
        }
    }

    @Composable
    private fun Map(
        modifier: Modifier = Modifier,
        initialLocationModel: LocationModel?,
        onMapReady: (mapView: MapView) -> Unit
    ) {
        val locationProvider = getKoin().get<LocationProvider>()
        val scope = rememberCoroutineScope()

        MapView(modifier = modifier) { mapView ->
            onMapReady.invoke(mapView)
            mapView.setMultiTouchControls(true)
            mapView.minZoomLevel = MapConfig.MIN_MAP_ZOOM
            mapView.maxZoomLevel = MapConfig.MAX_MAP_ZOOM
            if (initialLocationModel != null) {
                mapView.controller.setCenter(GeoPoint(initialLocationModel.lat, initialLocationModel.lng))
                mapView.controller.setZoom(MapConfig.DEFAULT_MAP_ZOOM)
            } else {
                scope.launch {
                    locationProvider.fetchOnce()
                    locationProvider.observeLocation()
                        .filterNotNull()
                        .take(1)
                        .collect { location ->
                            mapView.controller.setZoom(MapConfig.DEFAULT_MAP_ZOOM)
                            mapView.controller.setCenter(GeoPoint(location.location))
                        }
                }
            }
        }
    }
}