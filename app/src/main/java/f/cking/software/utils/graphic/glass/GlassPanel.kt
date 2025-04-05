package f.cking.software.utils.graphic.glass

import android.content.Context.SENSOR_SERVICE
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import f.cking.software.dpToPx
import f.cking.software.utils.graphic.glass.GlassShader.CurveType.Companion.getType
import kotlin.concurrent.timer
import kotlin.math.max
import kotlin.math.min

@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.glassPanel(
    rect: Rect,
    material: RefractionMaterial = RefractionMaterial.GLASS,
    aberrationIndex: Float = 0.1f,
    curveType: GlassShader.CurveType = GlassShader.CurveType.Mod,
    elevationPx: Float = LocalContext.current.dpToPx(8f).toFloat(),
    blurRadius: Float = 0f,
    tilt: Tilt = Tilt.Fixed(),
): Modifier =
    this.glassPanel(
        rect = rect,
        refractionIndex = material.refractionIndex,
        aberrationIndex = aberrationIndex,
        curveType = curveType,
        elevationPx = elevationPx,
        blurRadius = blurRadius,
        tilt = tilt,
    )

@Composable
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.glassPanel(
    rect: Rect,
    refractionIndex: Float = RefractionMaterial.GLASS.refractionIndex,
    aberrationIndex: Float = 0.1f,
    curveType: GlassShader.CurveType = GlassShader.CurveType.Mod,
    elevationPx: Float = LocalContext.current.dpToPx(8f).toFloat(),
    blurRadius: Float = 0f,
    tilt: Tilt = Tilt.Fixed(),
): Modifier =
    composed {
        var glassShader = remember { RuntimeShader(GlassShader.GLASS_SHADER_ADVANCED) }
        var contentSize by remember { mutableStateOf(Size(0.0f, 0.0f)) }
        var tiltFixed: Tilt.Fixed by remember { mutableStateOf(tilt as? Tilt.Fixed ?: Tilt.Fixed()) }

        if (tilt is Tilt.Motion) {
            MonitorTilt { x, y ->
                tiltFixed = Tilt.Fixed(limitTilt(tiltFixed.x + x * tilt.xMultiplayer), limitTilt(tiltFixed.y + y * tilt.yMultiplayer))
            }

            LaunchedEffect(Unit) {
                timer(period = 16) {
                    // slowly return tilt to 0
                    tiltFixed = Tilt.Fixed(tiltFixed.x * 0.99f, tiltFixed.y * 0.99f)
                }
            }
        }

        glassShader.setFloatUniform(GlassShader.ARG_ELEVATION, elevationPx)
        glassShader.setFloatUniform(GlassShader.ARG_REFRACTION_INDEX, refractionIndex)
        glassShader.setIntUniform(GlassShader.ARG_CURVE_TYPE, curveType::class.getType())
        glassShader.setFloatUniform(GlassShader.ARG_CURVE_PARAM_A, curveType.A)
        glassShader.setFloatUniform(GlassShader.ARG_CURVE_PARAM_K, curveType.k)
        glassShader.setFloatUniform(GlassShader.ARG_BLUR, blurRadius)
        glassShader.setFloatUniform(GlassShader.ARG_ABERRATION_INDEX, aberrationIndex)

        glassShader.setFloatUniform(GlassShader.ARG_RESOLUTION, contentSize.width.toFloat(), contentSize.height.toFloat())
        glassShader.setFloatUniform(GlassShader.ARG_PANEL_HEIGHT, rect.height().toFloat())
        glassShader.setFloatUniform(GlassShader.ARG_PANEL_WIDTH, rect.width().toFloat())
        glassShader.setFloatUniform(GlassShader.ARG_PANEL_X, rect.left.toFloat())
        glassShader.setFloatUniform(GlassShader.ARG_PANEL_Y, rect.top.toFloat())
        glassShader.setFloatUniform(GlassShader.ARG_TILT, tiltFixed.x, tiltFixed.y)

        this
            .onSizeChanged {
                contentSize = Size(it.width.toFloat(), it.height.toFloat())
            }.then(
                graphicsLayer {
                    renderEffect =
                        RenderEffect
                            .createRuntimeShaderEffect(glassShader, GlassShader.ARG_CONTENT)
                            .asComposeRenderEffect()
                },
            )
    }

@Composable
private fun MonitorTilt(onTiltChanged: (x: Float, y: Float) -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }

    DisposableEffect(Unit) {
        val listener =
            object : SensorEventListener {
                private var lastTimestamp: Long? = null

                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        val timestamp = it.timestamp
                        if (lastTimestamp != null) {
                            val deltaTime = (timestamp - lastTimestamp!!) / 1_000_000_000f // Convert nanoseconds to seconds

                            // Integrate the angular velocity to calculate tilt changes
                            val x = it.values[1] * deltaTime // Rotation around the X-axis
                            val y = it.values[0] * deltaTime // Rotation around the Y-axis

                            onTiltChanged(x, y)
                        }
                        lastTimestamp = timestamp
                    }
                }

                override fun onAccuracyChanged(
                    sensor: Sensor?,
                    accuracy: Int,
                ) {
                }
            }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
}

private fun limitTilt(value: Float): Float = min(max(value, -0.05f), 0.05f)

sealed interface Tilt {
    data class Fixed(
        @FloatRange(from = -1.0, to = 1.0) val x: Float = 0f,
        @FloatRange(from = -1.0, to = 1.0) val y: Float = 0f,
    ) : Tilt

    data class Motion(
        val xMultiplayer: Float = 0.05f,
        val yMultiplayer: Float = 0.02f,
    ) : Tilt

    companion object {
        val Motion = Motion()
    }
}

enum class RefractionMaterial(
    val refractionIndex: Float,
) {
    VACUUM(1.0f),
    AIR(1.0003f),
    ICE(1.31f),
    WATER(1.33f),
    GLASS(1.5f),
    SAPPHIRE(1.77f),
    PLASTIC(1.5f),
    DIAMOND(2.42f),
}
