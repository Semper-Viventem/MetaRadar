package f.cking.software.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import f.cking.software.R
import f.cking.software.utils.graphic.RoundedBox
import f.cking.software.utils.graphic.Switcher
import org.koin.androidx.compose.koinViewModel
import org.osmdroid.views.MapView


/**
 * A composable Google Map.
 * @author Arnau Mora
 * @since 20211230
 * @param modifier Modifiers to apply to the map.
 * @param onLoad This will get called once the map has been loaded.
 */
@Composable
fun MapView(
    modifier: Modifier = Modifier,
    onLoad: ((map: MapView) -> Unit)? = null,
    onUpdate: ((map: MapView) -> Unit)? = null,
) {
    val viewModel: MapViewModel = koinViewModel()

    if (viewModel.silentModeEnabled) {
        SilentModeDisclaimer(modifier = modifier, viewModel = viewModel)
    } else {
        OSMMap(modifier = modifier, viewModel = viewModel, onLoad = onLoad, onUpdate = onUpdate)
    }
}

@Composable
fun SilentModeDisclaimer(modifier: Modifier, viewModel: MapViewModel) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            modifier = Modifier.fillMaxSize().alpha(0.5f),
            painter = painterResource(id = R.drawable.map_placeholder),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
        RoundedBox(internalPaddings = 0.dp) {
            Text(modifier = Modifier.padding(16.dp), text = stringResource(id = R.string.silent_mode_is_enabled_disclaimer))
            Spacer(modifier = Modifier.height(8.dp))
            Switcher(
                value = viewModel.silentModeEnabled,
                title = stringResource(id = R.string.silent_mode_title),
                subtitle = stringResource(id = R.string.silent_mode_subtitle),
                onClick = {
                    viewModel.changeSilentModeState()
                }
            )
        }
    }
}

@Composable
fun OSMMap(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel,
    onLoad: ((map: MapView) -> Unit)? = null,
    onUpdate: ((map: MapView) -> Unit)? = null,
) {
    val mapViewState = rememberMapViewWithLifecycle()
    Box(modifier = modifier) {
        AndroidView(
            factory = { mapViewState.apply { onLoad?.invoke(this) } },
            update = { mapView -> onUpdate?.invoke(mapView) },
        )
        Text(
            text = stringResource(R.string.osm_copyright),
            modifier = Modifier
                .padding(start = 4.dp)
                .align(Alignment.BottomStart)
                .alpha(0.9f)
                .clickable { viewModel.openOSMLicense() },
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            id = R.id.layout_map
            clipToOutline = true
        }
    }

    // Makes MapView follow the lifecycle of this composable
    val lifecycleObserver = rememberMapLifecycleObserver(mapView)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}

@Composable
fun rememberMapLifecycleObserver(mapView: MapView): LifecycleEventObserver {
    return remember(mapView) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
    }
}

