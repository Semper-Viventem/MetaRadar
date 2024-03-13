package f.cking.software.ui.main

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import f.cking.software.utils.graphic.Shaders
import kotlin.math.pow

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun Modifier.withDropEffect(dropEffectState: DropEffectState): Modifier = composed {

    val shader = remember { RuntimeShader(Shaders.WATER_DROP) }
    val dropWave = remember { Animatable(0f) }

    val dropEvent = dropEffectState.dropEvent
    if (dropEvent != null) {
        LaunchedEffect(dropEffectState.dropEvent) {
            dropWave.snapTo(0f)
            dropWave.animateTo(1f, animationSpec = tween(durationMillis = 2000))
        }

        shader.setFloatUniform(
            "dropPosition",
            dropEvent.x,
            dropEvent.y,
        )

        shader.setFloatUniform("timeFactor", ((System.currentTimeMillis() - dropEvent.time) / 1000f + 2f))
    }

    val factor = -4f * dropWave.value.pow(2f) + 4 * dropWave.value
    shader.setFloatUniform("factor", factor)

    this
        .onSizeChanged {
            shader.setFloatUniform(
                "iResolution",
                it.width.toFloat(),
                it.height.toFloat(),
            )
            shader.setFloatUniform(
                "dropPosition",
                it.width.toFloat() / 2,
                it.height.toFloat() / 2,
            )
        }
        .then(
            graphicsLayer {
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "content")
                    .asComposeRenderEffect()
            }
        )
}

class DropEffectState {

    var dropEvent: DropEvent? by mutableStateOf(null)
        private set

    fun drop(centerX: Float, centerY: Float) {
        dropEvent = DropEvent(System.currentTimeMillis(), centerX, centerY)
    }
}

data class DropEvent(val time: Long, val x: Float, val y: Float)

@Composable
fun rememberDropEffectState(): DropEffectState {
    return remember { DropEffectState() }
}