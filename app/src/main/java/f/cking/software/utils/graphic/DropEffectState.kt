package f.cking.software.utils.graphic

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
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
import timber.log.Timber

@Composable
fun Modifier.withDropEffect(dropEffectState: DropEffectState): Modifier = composed {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed this
    }

    val shader = remember { RuntimeShader(Shaders.WATER_DROP) }
    val dropWave = remember { Animatable(0f) }

    val dropEvent = dropEffectState.dropEvent
    if (dropEvent != null) {
        LaunchedEffect(dropEffectState.dropEvent) {
            dropEvent.type.animSpecs.forEach { animationSpec ->
                dropWave.animateTo(
                    targetValue = animationSpec.to,
                    animationSpec = tween(durationMillis = animationSpec.duration, easing = animationSpec.easing)
                )
            }
            dropEvent.type.postFactor?.let { dropWave.snapTo(it) }
        }

        shader.setFloatUniform(
            "dropPosition",
            dropEvent.x,
            dropEvent.y,
        )
    }

    shader.setFloatUniform("factor", dropWave.value)

    Timber.d("DROP EFFECT: event: ${dropEvent?.type?.name}, factor: ${dropWave.value}")

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

    fun drop(centerX: Float, centerY: Float, type: DropEvent.Type) {
        dropEvent = DropEvent(System.currentTimeMillis(), centerX, centerY, type)
    }

    data class DropEvent(val time: Long, val x: Float, val y: Float, val type: Type) {
        enum class Type(val animSpecs: List<AnimationSpec>, val postFactor: Float? = null) {
            TOUCH(animSpecs = listOf(AnimationSpec(to = -1.5f, duration = 100))),
            RELEASE_SOFT(animSpecs = listOf(AnimationSpec(to = 0f, duration = 100))),
            RELEASE_HARD(animSpecs = listOf(AnimationSpec(to = 2f, duration = 1000, easing = CubicBezierEasing(0f, 0.5f, 0.75f, 1f))), postFactor = 0f),
        }

        data class AnimationSpec(val to: Float, val duration: Int, val easing: Easing = LinearEasing)
    }
}

@Composable
fun rememberDropEffectState(): DropEffectState {
    return remember { DropEffectState() }
}