package f.cking.software.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import f.cking.software.R
import f.cking.software.domain.model.DeviceData
import f.cking.software.toHexString
import org.osmdroid.views.MapView
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun rememberDateDialog(
    initialDate: LocalDate = LocalDate.now(),
    dateResult: (date: LocalDate) -> Unit,
    onDialogClosed: () -> Unit,
): MaterialDialogState {
    val dialogState = rememberMaterialDialogState()
    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton("Ok") { onDialogClosed.invoke() }
            negativeButton("Cancel") { onDialogClosed.invoke() }
        },
        onCloseRequest = {
            it.hide()
            onDialogClosed.invoke()
        },
    ) {
        datepicker(initialDate = initialDate) { localDate ->
            dateResult.invoke(localDate)
        }
    }
    return dialogState
}

@Composable
fun rememberTimeDialog(
    initialTime: LocalTime = LocalTime.now(),
    dateResult: (date: LocalTime) -> Unit,
    onDialogClosed: () -> Unit,
): MaterialDialogState {
    val dialogState = rememberMaterialDialogState()
    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton("Ok") { onDialogClosed.invoke() }
            negativeButton("Cancel") { onDialogClosed.invoke() }
        },
        onCloseRequest = {
            it.hide()
            onDialogClosed.invoke()
        },
    ) {
        timepicker(is24HourClock = true, initialTime = initialTime) { localDate ->
            dateResult.invoke(localDate)
        }
    }
    return dialogState
}

@Composable
fun ClickableField(
    text: String?,
    placeholder: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    TextField(
        modifier = modifier
            .onFocusChanged {
                if (it.isFocused) {
                    onClick.invoke()
                    focusManager.clearFocus(true)
                }
            },
        value = text ?: "",
        onValueChange = {},
        readOnly = true,
        placeholder = { placeholder?.let { Text(text = it) } },
    )
}

@Composable
fun DeviceListItem(
    device: DeviceData,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick.invoke() },
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row() {
                    Text(text = device.name ?: "N/A", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                device.manufacturerInfo?.name?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = it)
                }
                device.manufacturerInfo?.airdrop?.let { airdrop ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = airdrop.contacts.joinToString { "0x${it.sha256.toHexString().uppercase()}" })
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = device.address,
                    fontWeight = FontWeight.Light
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "lifetime: ${device.firstDetectionPeriod()} | last update: ${device.lastDetectionPeriod()} ago",
                    fontWeight = FontWeight.Light
                )
            }
            if (device.favorite) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Filled.Star, contentDescription = "Favorite")
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
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
fun rememberMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
    remember(mapView) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
    }

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
    onLoad: ((map: MapView) -> Unit)? = null
) {
    val mapViewState = rememberMapViewWithLifecycle()

    AndroidView(
        { mapViewState },
        modifier
    ) { mapView -> onLoad?.invoke(mapView) }
}

@Composable
fun Divider() {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Box(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(Color.LightGray)
        )
    }
}