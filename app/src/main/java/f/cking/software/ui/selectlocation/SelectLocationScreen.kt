package f.cking.software.ui.selectlocation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import f.cking.software.R
import f.cking.software.common.MapView
import f.cking.software.data.helpers.LocationProvider
import f.cking.software.domain.model.LocationModel
import f.cking.software.ui.devicedetails.MapConfig
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getKoin
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

object SelectLocationScreen {

    @Composable
    fun Screen(
        initialLocationModel: LocationModel?,
        onSelected: (location: LocationModel, radiusMeters: Float) -> Unit,
        onCloseClick: () -> Unit,
    ) {
        Scaffold(
            topBar = { AppBar(onCloseClick) },
            content = { paddings ->
                Content(modifier = Modifier.padding(paddings), onSelected = onSelected, initialLocationModel = initialLocationModel)
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
        onSelected: (location: LocationModel, radiusMeters: Float) -> Unit
    ) {
        val map = remember { mutableStateOf<MapView?>(null) }
        val locationProvider = getKoin().get<LocationProvider>()
        val scope = rememberCoroutineScope()

        Column(
            modifier = modifier
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            MapView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { mapView ->
                map.value = mapView
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
            Surface(elevation = 12.dp) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(MaterialTheme.colors.background)
                        .fillMaxWidth(),
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = map.value != null,
                        onClick = {
                            val cameraCenter = map.value!!.mapCenter
                            onSelected.invoke(LocationModel(cameraCenter.latitude, cameraCenter.longitude, System.currentTimeMillis()), 100f)
                        }
                    ) {
                        Text(text = stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}