package f.cking.software.utils.graphic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import f.cking.software.dpToPx
import f.cking.software.openUrl
import f.cking.software.pxToDp
import f.cking.software.toHexString
import f.cking.software.ui.GlobalUiState
import org.osmdroid.views.MapView
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

@Composable
fun rememberDateDialog(
    initialDate: LocalDate = LocalDate.now(),
    dateResult: (date: LocalDate) -> Unit,
): MaterialDialogState {
    val dialogState = rememberMaterialDialogState()
    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton(
                stringResource(R.string.ok),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.secondaryContainer)
            ) { dialogState.hide() }
            negativeButton(
                stringResource(R.string.cancel),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.secondaryContainer)
            ) { dialogState.hide() }
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
): MaterialDialogState {
    val dialogState = rememberMaterialDialogState()
    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton(
                stringResource(R.string.ok),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.secondaryContainer)
            ) { dialogState.hide() }
            negativeButton(
                stringResource(R.string.cancel),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.secondaryContainer)
            ) { dialogState.hide() }
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
    modifier: Modifier = Modifier,
    text: String?,
    placeholder: String?,
    label: String?,
    onClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val unfocuse = remember { mutableStateOf(false) }
    if (unfocuse.value) {
        focusManager.clearFocus(true)
        unfocuse.value = false
    }
    TextField(
        modifier = modifier
            .onFocusChanged {
                if (it.isFocused) {
                    unfocuse.value = true
                    onClick.invoke()
                }
            },
        value = text ?: "",
        onValueChange = {},
        readOnly = true,
        label = label?.let { { Text(text = it) } },
        placeholder = placeholder?.let { { Text(text = it) } },
    )
}

@Composable
fun DeviceListItem(
    device: DeviceData,
    onTagSelected: (tag: String) -> Unit = {},
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick.invoke() },
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = device.name ?: stringResource(R.string.not_applicable), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                device.tags.forEach {
                    Spacer(modifier = Modifier.width(4.dp))
                    TagChip(tagName = it, onClick = { onTagSelected.invoke(it) })
                }
                if (device.favorite) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = stringResource(R.string.is_favorite),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
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
                fontWeight = FontWeight.Light,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.lifetime_data,
                    device.firstDetectionPeriod(LocalContext.current),
                    device.lastDetectionPeriod(LocalContext.current)
                ),
                fontWeight = FontWeight.Light,
            )
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
    onLoad: ((map: MapView) -> Unit)? = null,
    onUpdate: ((map: MapView) -> Unit)? = null,
) {
    val mapViewState = rememberMapViewWithLifecycle()
    val context = LocalContext.current
    Box(modifier = modifier) {
        AndroidView(
            { mapViewState.apply { onLoad?.invoke(this) } },
        ) { mapView -> onUpdate?.invoke(mapView) }
        Text(
            text = stringResource(R.string.osm_copyright),
            modifier = Modifier
                .padding(start = 4.dp)
                .align(Alignment.BottomStart)
                .alpha(0.9f)
                .clickable { context.openUrl("https://www.openstreetmap.org/copyright") },
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
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

@Composable
fun ContentPlaceholder(
    text: String,
    modifier: Modifier = Modifier,
    icon: Painter = painterResource(R.drawable.ic_ghost),
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                modifier = Modifier.size(100.dp),
                painter = icon,
                contentDescription = text,
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = text, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun RoundedBox(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    internalPaddings: Dp = 16.dp,
    boxContent: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier) {
        val shape = RoundedCornerShape(corner = CornerSize(8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surfaceContainer, shape = shape)
                .clip(shape = shape)
                .padding(internalPaddings)
        ) { boxContent(this) }
    }
}

private val colors = listOf(
    Color(0xFFE57373),
    Color(0xFFF06292),
    Color(0xFFBA68C8),
    Color(0xFF9575CD),
    Color(0xFF7986CB),
    Color(0xFF64B5F6),
    Color(0xFF4FC3F7),
    Color(0xFF4DD0E1),
    Color(0xFF4DB6AC),
    Color(0xFF81C784),
    Color(0xFFAED581),
    Color(0xFFFF8A65),
    Color(0xFFD4E157),
    Color(0xFFFFD54F),
    Color(0xFFFFB74D),
    Color(0xFFA1887F),
    Color(0xFF90A4AE),
)

fun colorByHash(hash: Int): Color {
    return colors[abs(hash % colors.size)]
}

@Composable
fun TagChip(
    tagName: String,
    tagIcon: ImageVector? = null,
    onClick: () -> Unit = {},
) {
    AssistChip(
        colors = AssistChipDefaults.assistChipColors(
            containerColor = colorByHash(tagName.hashCode()),
            labelColor = Color.Black,
            leadingIconContentColor = Color.Black,
        ),
        border = null,
        onClick = onClick,
        leadingIcon = { tagIcon?.let { Icon(imageVector = it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) } },
        label = {
            Text(text = tagName)
        }
    )
}

@Composable
fun dpToPx(dp: Float): Float {
    return LocalContext.current.dpToPx(dp).toFloat()
}

@Composable
fun pxToDp(px: Float): Float {
    return LocalContext.current.pxToDp(px)
}

@Composable
fun BottomNavigationSpacer() {
    val bottomOffset = remember { GlobalUiState.navbarOffsetPx }
    Column {
        Spacer(modifier = Modifier.height(pxToDp(bottomOffset.value).dp))
    }
}

@Composable
fun FABSpacer() {
    val bottomOffset = remember { GlobalUiState.totalOffset }
    Column {
        Spacer(modifier = Modifier.height(pxToDp(bottomOffset.value).dp))
        SystemNavbarSpacer()
    }
}

@Composable
fun SystemNavbarSpacer() {
    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
}

fun ColorScheme.surfaceEvaluated(evaluation: Dp = 3.dp): Color {
    return this.surfaceColorAtElevation(evaluation)
}