package f.cking.software.utils.graphic

import android.graphics.BlendMode
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import f.cking.software.dpToPx


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
        val navbarHeightPx = remember { mutableStateOf(height?.value?.let(context::dpToPx)?.toFloat()) }
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
        RuntimeShader(Shaders.SHADER_CONTENT).apply {
            setFloatUniform("blurredHeight", heightPx)
        }
    }

    val blurredShader = remember {
        RuntimeShader(Shaders.SHADER_BLURRED).apply {
            setFloatUniform("blurredHeight", heightPx)
        }
    }

    this.onSizeChanged {
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