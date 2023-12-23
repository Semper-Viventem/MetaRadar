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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
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
fun GlassBottomNavBar(
    modifier: Modifier = Modifier,
    blur: Float = 3f,
    glassCurveSizeDp: Float = 3f,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    overlayColor: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f),
    content: @Composable () -> Unit,
) {
    GlassBottomSpace(
        modifier = modifier,
        blur = blur,
        glassCurveSizeDp = glassCurveSizeDp,
        fallbackColor = fallbackColor,
        overlayColor = overlayColor,
        bottomContent = { BottomNavigationSpacer() }
    ) {
        content()
    }
}

@Composable
fun GlassSystemNavbar(
    modifier: Modifier = Modifier,
    blur: Float = 3f,
    glassCurveSizeDp: Float = 3f,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    overlayColor: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    GlassBottomSpace(
        modifier = modifier,
        blur = blur,
        glassCurveSizeDp = glassCurveSizeDp,
        fallbackColor = fallbackColor,
        overlayColor = overlayColor,
        bottomContent = { SystemNavbarSpacer() }
    ) {
        content()
    }
}

@Composable
fun GlassBottomSpace(
    modifier: Modifier = Modifier,
    height: Dp? = null,
    blur: Float = 3f,
    glassCurveSizeDp: Float = 3f,
    zIndex: Float = 1f,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    overlayColor: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f),
    bottomContent: @Composable () -> Unit,
    globalContent: @Composable () -> Unit,
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
                        it.blurBottom(heightPx = navbarHeightPx.value!!, blur = blur, glassCurveSizeDp = glassCurveSizeDp)
                    } else it
                }
        ) {
            globalContent()
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
            bottomContent()

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.2f), Color.Transparent)))
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.blurBottom(heightPx: Float, blur: Float, glassCurveSizeDp: Float): Modifier = composed {
    val context = LocalContext.current

    val contentShader = remember { RuntimeShader(Shaders.SHADER_CONTENT) }
    val blurredShader = remember { RuntimeShader(Shaders.SHADER_BLURRED) }
    val glassShader = remember { RuntimeShader(Shaders.GLASS_SHADER) }

    contentShader.setFloatUniform("blurredHeight", heightPx)
    blurredShader.setFloatUniform("blurredHeight", heightPx)
    glassShader.setFloatUniform("blurredHeight", heightPx)
    glassShader.setFloatUniform("horizontalSquareSize", context.dpToPx(glassCurveSizeDp).toFloat())

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
            glassShader.setFloatUniform(
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
                            RenderEffect.createChainEffect(
                                RenderEffect.createRuntimeShaderEffect(glassShader, "content"),
                                RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.MIRROR),
                            )
                        ),
                        BlendMode.SRC_OVER,
                    )
                    .asComposeRenderEffect()
            }
        )
}