package f.cking.software.ui.shadertest

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import f.cking.software.BuildConfig
import f.cking.software.R
import f.cking.software.letIf
import f.cking.software.utils.graphic.GlassSystemNavbar
import f.cking.software.utils.graphic.glass.GlassShader
import f.cking.software.utils.graphic.glass.RefractionMaterial
import f.cking.software.utils.graphic.glass.Tilt
import f.cking.software.utils.graphic.glass.glassPanel
import f.cking.software.utils.graphic.pxToDp
import f.cking.software.utils.navigation.BackCommand
import f.cking.software.utils.navigation.Router
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
object ShaderTestScreen {
    @Composable
    fun Screen(router: Router) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Text("This screen requires Android 13+")
            return
        }

        if (!BuildConfig.DEBUG) {
            Text("This is screen is for debugging purposes only. Not supported in release builds.")
            return
        }

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        Scaffold(
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { AppBar(scrollBehavior) { router.navigate(BackCommand) } },
            content = { paddings ->
                GlassSystemNavbar(
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        Content(Modifier.padding(top = paddings.calculateTopPadding()))
                    },
                )
            },
        )
    }

    @SuppressLint("NewApi")
    @Composable
    private fun Content(parentModifier: Modifier = Modifier) {
        val scrollState = rememberScrollState()
        var screenSize by remember { mutableStateOf(Size(0.0f, 0.0f)) }
        var settingsBlockSize by remember { mutableStateOf(Size(0.0f, 0.0f)) }
        var tilt: Tilt.Fixed by remember { mutableStateOf(Tilt.Fixed()) }

        val sliderSizeState = remember { SliderState(value = 0.5f) }
        val sliderAberrationState = remember { SliderState(value = 0.1f) }
        val sliderBlurRadiusState = remember { SliderState(value = 3f, valueRange = 0f..8f) }
        val sliderAmplitudeState = remember { SliderState(value = 0.3f) }
        val sliderLengthState = remember { SliderState(value = 0.2f) }
        val sliderRefractionIndexState =
            remember {
                SliderState(
                    value = RefractionMaterial.GLASS.refractionIndex,
                    valueRange = RefractionMaterial.VACUUM.refractionIndex..RefractionMaterial.DIAMOND.refractionIndex,
                )
            }

        var glassType by remember { mutableStateOf<GlassType>(GlassType.MOD) }

        val offset = max(0f, settingsBlockSize.height - scrollState.value)

        val panelSize = Size(screenSize.width * sliderSizeState.value, screenSize.height * sliderSizeState.value)
        val top = max((screenSize.height / 2 - panelSize.height / 2).toInt(), offset.toInt())
        val rect =
            Rect(
                (screenSize.width / 2 - panelSize.width / 2).toInt(),
                top,
                (screenSize.width / 2 + panelSize.width / 2).toInt(),
                (top + panelSize.height).toInt(),
            )

        Box(
            parentModifier
                .fillMaxSize()
                .onSizeChanged {
                    screenSize = Size(it.width.toFloat(), it.height.toFloat())
                },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .letIf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        it.glassPanel(
                            rect = rect,
                            refractionIndex = sliderRefractionIndexState.value,
                            aberrationIndex = sliderAberrationState.value,
                            curveType = glassType.curveType(sliderAmplitudeState.value, sliderLengthState.value),
                            blurRadius = sliderBlurRadiusState.value,
                            tilt = glassType.motion,
                        )
                    }.verticalScroll(scrollState),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged { settingsBlockSize = Size(it.width.toFloat(), it.height.toFloat()) },
                ) {
                    // Scale
                    Slider(
                        text = stringResource(R.string.shader_test_panel_size, (sliderSizeState.value * 100).toInt()),
                        state = sliderSizeState,
                    )

                    // Aberraion
                    Slider(
                        text = stringResource(R.string.shader_test_glass_aberration, sliderAberrationState.value),
                        state = sliderAberrationState,
                    )

                    // Refraction Index
                    Slider(
                        text = stringResource(R.string.shader_test_glass_refraction, sliderRefractionIndexState.value),
                        state = sliderRefractionIndexState,
                    )

                    // Blur radius
                    Slider(
                        text = stringResource(R.string.shader_test_blur_radius, sliderBlurRadiusState.value),
                        state = sliderBlurRadiusState,
                    )

                    // Glass type
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        text = stringResource(R.string.shader_test_glass_type),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Absolute.SpaceAround) {
                        GlassType.entries.forEach { type ->
                            FilterChip(
                                onClick = { glassType = type },
                                trailingIcon =
                                    type.imageRes?.let {
                                        {
                                            Icon(
                                                modifier = Modifier.size(32.dp),
                                                painter = painterResource(id = it),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                selected = glassType == type,
                                label = { Text(stringResource(type.nameRes)) },
                            )
                        }
                    }

                    AnimatedVisibility(glassType.curveSupport) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Curve amplitude
                            Slider(
                                text = stringResource(R.string.shader_test_curve_amplitude, sliderAmplitudeState.value),
                                state = sliderAmplitudeState,
                            )

                            // Curve length
                            Slider(
                                text = stringResource(R.string.shader_test_curve_length, sliderLengthState.value),
                                state = sliderLengthState,
                            )
                        }
                    }
                }

                // Images
                Image(
                    modifier = Modifier.fillMaxWidth(),
                    painter = painterResource(id = R.drawable.monstera),
                    contentDescription = "",
                    contentScale = ContentScale.FillWidth,
                )
                Image(
                    modifier = Modifier.fillMaxWidth(),
                    painter = painterResource(id = R.drawable.appa),
                    contentDescription = stringResource(id = R.string.secret_cat),
                    contentScale = ContentScale.FillWidth,
                )
                Spacer(modifier = Modifier.height(200.dp))
            }

            Box(
                Modifier
                    .size(
                        height = pxToDp(rect.height().toFloat()).dp,
                        width = pxToDp(rect.width().toFloat()).dp,
                    ).offset(
                        x = pxToDp(rect.left.toFloat()).dp,
                        y = pxToDp(rect.top.toFloat()).dp,
                    ).background(Color.Gray.copy(alpha = 0.1f)),
            )

            Text(
                modifier = Modifier.align(Alignment.BottomStart).padding(32.dp),
                color = MaterialTheme.colorScheme.onSurface,
                text = stringResource(R.string.shader_test_tilt, tilt.x, tilt.y),
            )
        }
    }

    private enum class GlassType(
        val curveType: (A: Float, k: Float) -> GlassShader.CurveType,
        val nameRes: Int,
        val imageRes: Int?,
        val curveSupport: Boolean,
        val motion: Tilt.Motion = Tilt.Motion,
    ) {
        MOD({
            A,
            k,
            ->
            GlassShader.CurveType.Mod(A, k)
        }, R.string.shader_test_glass_type_mod, R.drawable.glass_type_fluted, curveSupport = true),
        SIN({
            A,
            k,
            ->
            GlassShader.CurveType.Sin(A, k)
        }, R.string.shader_test_glass_type_sin, R.drawable.glass_type_curved, curveSupport = true),
        FLAT({ _, _ -> GlassShader.CurveType.Flat }, R.string.shader_test_glass_type_flat, null, curveSupport = false),
    }

    @Composable
    private fun Slider(
        text: String,
        state: SliderState,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.SemiBold,
        )
        Slider(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            state = state,
        )
    }

    @Composable
    private fun AppBar(
        scrollState: TopAppBarScrollBehavior,
        onBackClick: () -> Unit,
    ) {
        TopAppBar(
            scrollBehavior = scrollState,
            colors =
                TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            title = { Text(text = stringResource(R.string.shader_test_title)) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
        )
    }
}
