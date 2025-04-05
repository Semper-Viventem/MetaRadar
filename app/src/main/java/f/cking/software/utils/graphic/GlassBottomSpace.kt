package f.cking.software.utils.graphic

import android.annotation.SuppressLint
import android.graphics.BlendMode
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Size
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
import f.cking.software.letIf
import f.cking.software.utils.graphic.glass.GlassShader
import f.cking.software.utils.graphic.glass.RefractionMaterial
import f.cking.software.utils.graphic.glass.Tilt
import f.cking.software.utils.graphic.glass.glassPanel
import kotlin.math.max

data object GlassBottomSpaceDefaults {
    const val BLUR = 4f
}

@Composable
fun GlassBottomNavBar(
    modifier: Modifier = Modifier,
    blur: Float = GlassBottomSpaceDefaults.BLUR,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    overlayColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f),
    content: @Composable (bottomPadding: PaddingValues) -> Unit,
) {
    GlassBottomSpace(
        modifier = modifier,
        blur = blur,
        fallbackColor = fallbackColor,
        overlayColor = overlayColor,
        bottomContent = { BottomNavigationSpacer() },
    ) { padding ->
        content(padding)
    }
}

@Composable
fun GlassSystemNavbar(
    modifier: Modifier = Modifier,
    blur: Float = GlassBottomSpaceDefaults.BLUR,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    overlayColor: Color = Color.Transparent,
    content: @Composable (bottomPadding: PaddingValues) -> Unit,
) {
    GlassBottomSpace(
        modifier = modifier,
        blur = blur,
        fallbackColor = fallbackColor,
        overlayColor = overlayColor,
        bottomContent = { SystemNavbarSpacer() },
    ) { padding ->
        content(padding)
    }
}

@SuppressLint("NewApi")
@Composable
fun GlassBottomSpace(
    modifier: Modifier = Modifier,
    height: Dp? = null,
    blur: Float = GlassBottomSpaceDefaults.BLUR,
    zIndex: Float = 1f,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    overlayColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f),
    bottomContent: @Composable () -> Unit,
    globalContent: @Composable (bottomPadding: PaddingValues) -> Unit,
) {
    Box(modifier = modifier) {
        val context = LocalContext.current
        var navbarHeightPx by remember { mutableStateOf(height?.value?.let(context::dpToPx)?.toFloat()) }
        val isRenderEffectSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .letIf(isRenderEffectSupported && navbarHeightPx != null) {
                        it.glassBottom(heightPx = navbarHeightPx!!, blurRadius = blur)
                    },
        ) {
            globalContent(PaddingValues(bottom = navbarHeightPx?.dp ?: 0.dp))
        }
        Box(
            modifier =
                Modifier
                    .zIndex(zIndex)
                    .fillMaxWidth()
                    .let {
                        if (height == null) {
                            it.onGloballyPositioned {
                                navbarHeightPx = it.size.height.toFloat()
                            }
                        } else {
                            it.height(height)
                        }
                    }.let {
                        if (!isRenderEffectSupported) {
                            it.background(fallbackColor)
                        } else {
                            it.background(overlayColor)
                        }
                    }.align(Alignment.BottomCenter),
        ) {
            Column {
                bottomContent()
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.2f), Color.Transparent)),
                    ),
            )
        }
    }
}

@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.glassBottom(
    heightPx: Float,
    curveType: GlassShader.CurveType = GlassShader.CurveType.Mod,
    elevationPx: Float = LocalContext.current.dpToPx(8f).toFloat(),
    blurRadius: Float = GlassBottomSpaceDefaults.BLUR,
): Modifier =
    composed {
        val contentSize = remember { mutableStateOf(Size(0.0f, 0.0f)) }

        this
            .onSizeChanged {
                contentSize.value = Size(it.width.toFloat(), it.height.toFloat())
            }.then(
                glassPanel(
                    rect =
                        Rect(
                            0,
                            (contentSize.value.height - heightPx).toInt(),
                            contentSize.value.width.toInt(),
                            contentSize.value.height.toInt(),
                        ),
                    curveType = curveType,
                    elevationPx = elevationPx,
                    material = RefractionMaterial.GLASS,
                    blurRadius = blurRadius,
                    tilt = Tilt.Motion(0.04f, 0.015f),
                ),
            )
    }

@Deprecated("Use glassBottom instead, I want to keep this code here for historical reasons", ReplaceWith("glassBottom()"))
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.blurBottom(
    heightPx: Float,
    blur: Float,
    glassCurveSizeDp: Float,
): Modifier =
    composed {
        val context = LocalContext.current

        val contentShader = remember { RuntimeShader(Shaders.SHADER_CONTENT) }
        val effectAreaShader = remember { RuntimeShader(Shaders.SHADER_EFFECT_AREA) }
        val glassShader = remember { RuntimeShader(Shaders.GLASS_SHADER) }

        contentShader.setFloatUniform("blurredHeight", heightPx)
        effectAreaShader.setFloatUniform("blurredHeight", heightPx)
        glassShader.setFloatUniform("blurredHeight", heightPx)

        this
            .onSizeChanged {
                contentShader.setFloatUniform(
                    "iResolution",
                    it.width.toFloat(),
                    it.height.toFloat(),
                )
                effectAreaShader.setFloatUniform(
                    "iResolution",
                    it.width.toFloat(),
                    it.height.toFloat(),
                )
                glassShader.setFloatUniform(
                    "iResolution",
                    it.width.toFloat(),
                    it.height.toFloat(),
                )

                val minCurveSizePx: Float = it.width / 100f
                val glassCurveSizePx = max(minCurveSizePx, context.dpToPx(glassCurveSizeDp).toFloat())
                glassShader.setFloatUniform("horizontalSquareSize", glassCurveSizePx)
            }.graphicsLayer {
                renderEffect =
                    RenderEffect
                        .createBlendModeEffect(
                            RenderEffect.createRuntimeShaderEffect(contentShader, Shaders.ARG_CONTENT),
                            RenderEffect.createChainEffect(
                                RenderEffect.createRuntimeShaderEffect(effectAreaShader, Shaders.ARG_CONTENT),
                                RenderEffect.createChainEffect(
                                    RenderEffect.createRuntimeShaderEffect(glassShader, Shaders.ARG_CONTENT),
                                    RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.MIRROR),
                                ),
                            ),
                            BlendMode.SRC_OVER,
                        ).asComposeRenderEffect()
            }
    }
