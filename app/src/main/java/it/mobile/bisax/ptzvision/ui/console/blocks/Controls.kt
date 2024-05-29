package it.mobile.bisax.ptzvision.ui.console.blocks

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.ui.unit.min as minDp

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun JoyStick(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    moved: (x: Float, y: Float) -> Unit = { _, _ -> },
    enabled: Boolean = true,
    hapticFeedbackEnabled: Boolean
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(15.dp)
    ) {
        val padSize = minDp(maxWidth, maxHeight)
        val dotSize: Dp = padSize / 3.5f
        val maxRadius = with(LocalDensity.current) { (padSize / 2).toPx() }
        val centerPx = with(LocalDensity.current) { ((padSize - dotSize) / 2).toPx() }

        var offsetX by remember { mutableFloatStateOf(centerPx) }
        var offsetY by remember { mutableFloatStateOf(centerPx) }
        var radius by remember { mutableFloatStateOf(0f) }
        var theta by remember { mutableFloatStateOf(0f) }

        var positionX by remember { mutableFloatStateOf(0f) }
        var positionY by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .size(padSize)
                .background(
                    Brush.radialGradient(
                        0.0f to Color.Gray,
                        1f to Color.DarkGray,
                        radius = 600.0f,
                        tileMode = TileMode.Repeated
                    ), CircleShape
                )
        ) {
            val vibe = LocalContext.current.getSystemService(Vibrator::class.java) as Vibrator
            val coroutine = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (positionX + centerPx).roundToInt(),
                            (positionY + centerPx).roundToInt()
                        )
                    }
                    .size(dotSize)
                    .background(
                        color = if (enabled) Color.Green else Color(0x88FFFFFF),
                        shape = CircleShape
                    )
                    .then (
                        if (enabled) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(onDragEnd = {
                                    vibe.cancel()
                                    offsetX = centerPx
                                    offsetY = centerPx
                                    radius = 0f
                                    theta = 0f
                                    positionX = 0f
                                    positionY = 0f

                                    coroutine.launch {
                                        //Log.d("JoyStick", "Setting pan tilt to 0,0")
                                        mainViewModel.setPanTilt(0f, 0f)
                                        delay(100)
                                        //Log.d("JoyStick", "Setting pan tilt to 0,0 again")
                                        mainViewModel.setPanTilt(0f, 0f)
                                    }
                                }) { pointerInputChange: PointerInputChange, offset: Offset ->
                                    val x = offsetX + offset.x - centerPx
                                    val y = offsetY + offset.y - centerPx

                                    pointerInputChange.consume()

                                    theta = if (x >= 0 && y >= 0) {
                                        atan(y / x)
                                    } else if (x < 0 && y >= 0) {
                                        PI.toFloat() + atan(y / x)
                                    } else if (x < 0 && y < 0) {
                                        -PI.toFloat() + atan(y / x)
                                    } else {
                                        atan(y / x)
                                    }

                                    radius = sqrt((x.pow(2)) + (y.pow(2)))

                                    offsetX += offset.x
                                    offsetY += offset.y


                                    val clampedRadius = minOf(radius, maxRadius)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hapticFeedbackEnabled) {
                                        vibe.vibrate(
                                            VibrationEffect.createWaveform(
                                                longArrayOf(
                                                    0,
                                                    ((clampedRadius / maxRadius) * 105).toLong() + 1L
                                                ),
                                                intArrayOf(
                                                    VibrationEffect.EFFECT_TICK,
                                                    VibrationEffect.EFFECT_TICK
                                                ),
                                                0
                                            )
                                        )
                                    }
                                    polarToCartesian(clampedRadius, theta).apply {
                                        positionX = first
                                        positionY = second

                                        val scaledDistance = clampedRadius / maxRadius
                                        polarToCartesian(scaledDistance, theta).apply {
                                            val posXinSquare = 0.5f * sqrt(
                                                2 + first.pow(2) - second.pow(2) + 2 * first * sqrt(2.0f)
                                            ) - 0.5f * sqrt(
                                                2 + first.pow(2) - second.pow(2) - 2 * first * sqrt(2.0f)
                                            )
                                            val posYinSquare = 0.5f * sqrt(
                                                2 - first.pow(2) + second.pow(2) + 2 * second * sqrt(2.0f)
                                            ) - 0.5f * sqrt(
                                                2 - first.pow(2) + second.pow(2) - 2 * second * sqrt(2.0f)
                                            )

                                            coroutine.launch {
                                                // Log.d("JoyStick", "Setting pan tilt to $posXinSquare, $posYinSquare")
                                                mainViewModel.setPanTilt(
                                                    posXinSquare,
                                                    -posYinSquare
                                                ) // negative y because of the inverted y axis in the UI space
                                            }
                                        }
                                    }

                                }
                            }
                        }
                        else{
                            Modifier
                        }
                    )
                    .onGloballyPositioned { coordinates ->
                        moved(
                            (coordinates.positionInParent().x - centerPx) / maxRadius,
                            -(coordinates.positionInParent().y - centerPx) / maxRadius
                        )
                    }
            )
        }
    }
}

private fun polarToCartesian(radius: Float, theta: Float): Pair<Float, Float> =
    Pair(radius * cos(theta), radius * sin(theta))

@Composable
fun SliderBox(
    modifier: Modifier = Modifier,
    moved: (y: Float) -> Unit = { _ -> },
    setPosition: (maxPos:Float, posY: Float) -> Unit,
    enabled: Boolean = false,
    hapticFeedbackEnabled: Boolean
){
    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        val sliderHeight = maxHeight
        val sliderWidth = maxHeight*0.3f
        val vibe = LocalContext.current.getSystemService(Vibrator::class.java) as Vibrator
        Box(
            modifier = Modifier
                .height(sliderHeight)
                .width(sliderWidth)
                .background(
                    Brush.radialGradient(
                        0.0f to Color.Gray,
                        1f to Color.DarkGray,
                        radius = 600.0f,
                        tileMode = TileMode.Repeated
                    ), CircleShape
                )
        ) {
            val dotSize: Dp = sliderWidth * 0.8f
            val maxYOffset =
                with(LocalDensity.current) { (sliderHeight / 2).toPx() - (dotSize / 2).toPx() }
            val centerX = with(LocalDensity.current) { ((sliderWidth - dotSize) / 2).toPx() }
            val centerY = with(LocalDensity.current) { ((sliderHeight - dotSize) / 2).toPx() }

            var offsetY by remember { mutableFloatStateOf(centerY) }

            var radius by remember { mutableFloatStateOf(0f) }
            var theta by remember { mutableFloatStateOf(0f) }

            var positionY by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            centerX.roundToInt(),
                            (positionY + centerY).roundToInt()
                        )
                    }
                    .size(dotSize)
                    .background(if (enabled) Color.Green else Color(0x88FFFFFF), CircleShape)
                    .then(
                        if (enabled) {
                            Modifier
                                .pointerInput(Unit) {
                                    detectDragGestures(onDragEnd = {
                                        vibe.cancel()

                                        offsetY = centerY
                                        radius = 0f
                                        theta = 0f
                                        positionY = 0f

                                        setPosition(1f, 0f)
                                    }) { pointerInputChange: PointerInputChange, offset: Offset ->
                                        val y = offsetY + offset.y - centerY

                                        pointerInputChange.consume()

                                        theta = PI.toFloat() / 2

                                        radius = y

                                        offsetY += offset.y

                                        val clampedRadius = sign(radius) * minOf(abs(radius), maxYOffset)

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hapticFeedbackEnabled) {
                                            vibe.vibrate(
                                                VibrationEffect.createWaveform(
                                                    longArrayOf(
                                                        0,
                                                        ((abs(clampedRadius) / maxYOffset) * 110).toLong() + 1L
                                                    ),
                                                    intArrayOf(
                                                        VibrationEffect.EFFECT_TICK,
                                                        VibrationEffect.EFFECT_TICK
                                                    ),
                                                    0
                                                )
                                            )
                                        }

                                        polarToCartesian(clampedRadius, theta).apply {
                                            val isMax = positionY == second
                                            positionY = second

                                            if (!isMax) {
                                                setPosition(
                                                    sliderHeight.toPx() / 2 - dotSize.toPx() / 2,
                                                    -positionY
                                                )
                                            }
                                        }
                                    }
                                }
                                .onGloballyPositioned { coordinates ->
                                    moved(
                                        -(coordinates.positionInParent().y - centerY) / maxYOffset
                                    )
                                }
                        } else {
                            Modifier
                        }
                    ),
            )
        }
    }
}