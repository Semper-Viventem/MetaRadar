package f.cking.software.common

import android.graphics.BlendMode
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
import org.intellij.lang.annotations.Language
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
            positiveButton(stringResource(R.string.ok)) { dialogState.hide() }
            negativeButton(stringResource(R.string.cancel)) { dialogState.hide() }
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
            positiveButton(stringResource(R.string.ok)) { dialogState.hide() }
            negativeButton(stringResource(R.string.cancel)) { dialogState.hide() }
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
                    Icon(imageVector = Icons.Filled.Star, contentDescription = stringResource(R.string.is_favorite))
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
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.lifetime_data,
                    device.firstDetectionPeriod(LocalContext.current),
                    device.lastDetectionPeriod(LocalContext.current)
                ),
                fontWeight = FontWeight.Light
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
    icon: Painter = painterResource(R.drawable.ic_ghost),
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
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
                .background(color = Color.LightGray, shape = shape)
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TagChip(
    tagName: String,
    tagIcon: ImageVector? = null,
    onClick: () -> Unit = {},
) {
    Chip(
        colors = ChipDefaults.chipColors(
            backgroundColor = colorByHash(tagName.hashCode()),
            contentColor = Color.Black,
            leadingIconContentColor = Color.Black,
        ),
        onClick = onClick,
        leadingIcon = { tagIcon?.let { Icon(imageVector = it, contentDescription = null) } },
    ) {
        Text(text = tagName)
    }
}

@Language("AGSL")
val SHADER_CONTENT = """
        uniform shader content;
        uniform shader blur;
    
        uniform float blurredHeight;
        uniform float2 iResolution;
        
        float4 main(float2 coord) {
            if (coord.y > iResolution.y - blurredHeight) { // Blur the bottom part of the screen
                return float4(1.0, 1.0, 1.0, 1.0);
            } else {
                return content.eval(coord);
            }
        }
"""

@Language("AGSL")
val SHADER_BLURRED = """
        uniform shader content;
    
        uniform float blurredHeight;
        uniform float2 iResolution;
        
        float4 main(float2 coord) {
            if (coord.y > iResolution.y - blurredHeight) { // Blur the bottom part of the screen
                return content.eval(coord);
            } else {
                return float4(0.0, 0.0, 0.0, 0.0);
            }
        }
    """

@Composable
fun BlurredNavBar(
    modifier: Modifier = Modifier,
    height: Dp? = null,
    blur: Float = 10f,
    zIndex: Float = 1f,
    fallbackColor: Color = MaterialTheme.colors.primary,
    overlayColor: Color = MaterialTheme.colors.primary.copy(alpha = 0.2f),
    navBarContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        val context = LocalContext.current
        var navbarHeightPx = remember { mutableStateOf(height?.value?.let(context::dpToPx)?.toFloat()) }
        val isRenderEffectSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (isRenderEffectSupported && navbarHeightPx.value != null) {
                        it.blurBottom(heightPx = navbarHeightPx.value!!, blur = blur)
                    } else it
                }
        ) {
            content()
        }
        Box(
            modifier = Modifier
                .zIndex(zIndex)
                .fillMaxWidth()
                .let {
                    if (height == null) {
                        it.onGloballyPositioned {
                            navbarHeightPx.value = it.size.height.toFloat()
                        }
                    } else {
                        it.height(height)
                    }
                }
                .let {
                    if (!isRenderEffectSupported) {
                        it.background(fallbackColor)
                    } else {
                        it.background(overlayColor)
                    }
                }
                .align(Alignment.BottomCenter)
        ) {
            navBarContent()

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.blurBottom(heightPx: Float, blur: Float): Modifier = composed {
    val contentShader = remember {
        RuntimeShader(SHADER_CONTENT).apply {
            setFloatUniform("blurredHeight", heightPx)
        }
    }

    val blurredShader = remember {
        RuntimeShader(SHADER_BLURRED).apply {
            setFloatUniform("blurredHeight", heightPx)
        }
    }

    this
        .onSizeChanged {
            contentShader.setFloatUniform(
                "iResolution",
                it.width.toFloat(),
                it.height.toFloat(),
            )
            blurredShader.setFloatUniform(
                "iResolution",
                it.width.toFloat(),
                it.height.toFloat(),
            )
        }
        .then(
            graphicsLayer {
                renderEffect = RenderEffect
                    .createBlendModeEffect(
                        RenderEffect.createRuntimeShaderEffect(contentShader, "content"),
                        RenderEffect.createChainEffect(
                            RenderEffect.createRuntimeShaderEffect(blurredShader, "content"),
                            RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.DECAL)
                        ),
                        BlendMode.SRC_OVER,
                    )
                    .asComposeRenderEffect()
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
fun BottomSpacer() {
    val bottomOffset = remember { GlobalUiState.totalOffset }
    Spacer(modifier = Modifier.height(pxToDp(bottomOffset.value).dp))
}