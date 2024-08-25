package it.mobile.bisax.ptzvision.ui.console.blocks

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import it.mobile.bisax.ptzvision.R
import it.mobile.bisax.ptzvision.controller.PTZController
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import kotlinx.coroutines.async
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


fun createVibration(vibePercentage: Float): VibrationEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
    val loopMs = 101

    val timings = LongArray(loopMs)
    val amplitudes = IntArray(loopMs)

    for (i in 0..<loopMs) {
        timings[i] = 1
        amplitudes[i] = ((1 + sin(i.toDouble() / 50 * PI)) * vibePercentage / 2 * 255).toInt()
    }

    return VibrationEffect.createWaveform(
        timings, amplitudes, loopMs-1
    )
}

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
                        0.0f to MaterialTheme.colorScheme.secondaryContainer,
                        1f to MaterialTheme.colorScheme.onSecondary,
                        radius = 600.0f,
                        tileMode = TileMode.Repeated
                    ), CircleShape
                )
        ) {
            val vibe = LocalContext.current.getSystemService(Vibrator::class.java) as Vibrator
            val coroutine = rememberCoroutineScope()



            LaunchedEffect(enabled){
                if(!enabled){
                    offsetX = centerPx
                    offsetY = centerPx
                    radius = 0f
                    theta = 0f
                    positionX = 0f
                    positionY = 0f

                    coroutine.launch {
                        mainViewModel.setPanTilt(0f, 0f)
                        vibe.cancel()
                        delay(100)
                        mainViewModel.setPanTilt(0f, 0f)
                        vibe.cancel()
                    }
                }
            }

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
                        if (enabled)
                            MaterialTheme.colorScheme.tertiary
                        else
                            Color(0x88FFFFFF),
                        CircleShape
                    )
                    .then(
                        if (enabled) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(onDragEnd = {
                                    offsetX = centerPx
                                    offsetY = centerPx
                                    radius = 0f
                                    theta = 0f
                                    positionX = 0f
                                    positionY = 0f

                                    coroutine.launch {
                                        mainViewModel.setPanTilt(0f, 0f)
                                        vibe.cancel()
                                        delay(100)
                                        mainViewModel.setPanTilt(0f, 0f)
                                        vibe.cancel()
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
                                    val vibePercentage = clampedRadius / maxRadius
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hapticFeedbackEnabled) {
                                        vibe.cancel()
                                        if ((vibePercentage * 24).toInt() > 0)
                                            vibe.vibrate(createVibration(vibePercentage))
                                    }
                                    polarToCartesian(clampedRadius, theta).apply {
                                        positionX = first
                                        positionY = second

                                        val scaledDistance = clampedRadius / maxRadius
                                        polarToCartesian(scaledDistance, theta).apply {
                                            val posXinSquare = 0.5f * sqrt(
                                                2 + first.pow(2) - second.pow(2) + 2 * first * sqrt(
                                                    2.0f
                                                )
                                            ) - 0.5f * sqrt(
                                                2 + first.pow(2) - second.pow(2) - 2 * first * sqrt(
                                                    2.0f
                                                )
                                            )
                                            val posYinSquare = 0.5f * sqrt(
                                                2 - first.pow(2) + second.pow(2) + 2 * second * sqrt(
                                                    2.0f
                                                )
                                            ) - 0.5f * sqrt(
                                                2 - first.pow(2) + second.pow(2) - 2 * second * sqrt(
                                                    2.0f
                                                )
                                            )

                                            coroutine.launch {
                                                mainViewModel.setPanTilt(
                                                    posXinSquare,
                                                    -posYinSquare
                                                ) // negative y because of the inverted y axis in the UI space
                                            }
                                        }
                                    }

                                }
                            }
                        } else {
                            Modifier
                        }
                    )
                    .onGloballyPositioned { coordinates ->
                        moved(
                            (coordinates.positionInParent().x - centerPx) / maxRadius,
                            -(coordinates.positionInParent().y - centerPx) / maxRadius
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.joystick_arrows),
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.onTertiary
                )
            }
        }
    }
}

private fun polarToCartesian(radius: Float, theta: Float): Pair<Float, Float> =
    Pair(radius * cos(theta), radius * sin(theta))

@Composable
private fun SliderBox(
    modifier: Modifier = Modifier,
    moved: (y: Float) -> Unit = { _ -> },
    setPosition: suspend (maxPos: Float, posY: Float) -> Unit,
    updateStatus: (suspend () -> Unit)? = null,
    enabled: Boolean = false,
    hapticFeedbackEnabled: Boolean
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight(),
    ) {
        val sliderHeight = maxHeight
        val sliderWidth = maxHeight * 0.3f

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
                .height(sliderHeight)
                .width(sliderWidth)
                .background(
                    Brush.radialGradient(
                        0.0f to MaterialTheme.colorScheme.secondaryContainer,
                        1f to MaterialTheme.colorScheme.onSecondary,
                        radius = 600.0f,
                        tileMode = TileMode.Repeated
                    ), CircleShape
                )
        ) {
            val vibe = LocalContext.current.getSystemService(Vibrator::class.java) as Vibrator
            val coroutine = rememberCoroutineScope()

            LaunchedEffect(positionY) {
                while (updateStatus != null ) {
                    if (positionY == 0f) {
                        updateStatus() // Box al centro
                        delay(1000L) // Delay di 1000ms
                    } else {
                        val x = async { updateStatus() }
                        x.await()
                    }
                }
            }

            LaunchedEffect(enabled){
                if(!enabled){
                    offsetY = centerY
                    radius = 0f
                    theta = 0f
                    positionY = 0f

                    coroutine.launch {
                        setPosition(1f, 0f)
                        vibe.cancel()
                        delay(100)
                        setPosition(1f, 0f)
                        vibe.cancel()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            centerX.roundToInt(),
                            (positionY + centerY).roundToInt()
                        )
                    }
                    .size(dotSize)
                    .background(
                        if (enabled)
                            MaterialTheme.colorScheme.tertiary
                        else
                            Color(0x88FFFFFF),
                        CircleShape
                    )
                    .then(
                        if (enabled) {
                            Modifier
                                .pointerInput(Unit) {
                                    detectDragGestures(onDragEnd = {
                                        offsetY = centerY
                                        radius = 0f
                                        theta = 0f
                                        positionY = 0f

                                        coroutine.launch {
                                            setPosition(1f, 0f)
                                            vibe.cancel()
                                            delay(100)
                                            setPosition(1f, 0f)
                                            vibe.cancel()
                                        }

                                    }) { pointerInputChange: PointerInputChange, offset: Offset ->
                                        val y = offsetY + offset.y - centerY

                                        pointerInputChange.consume()

                                        theta = PI.toFloat() / 2

                                        radius = y

                                        offsetY += offset.y

                                        val clampedRadius =
                                            sign(radius) * minOf(abs(radius), maxYOffset)
                                        val vibePercentage = abs(clampedRadius / maxYOffset)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hapticFeedbackEnabled) {
                                            vibe.cancel()
                                            if ((vibePercentage * 7).toInt() > 0)
                                                vibe.vibrate(createVibration(vibePercentage))
                                        }

                                        polarToCartesian(clampedRadius, theta).apply {
                                            val isMax = positionY == second
                                            positionY = second

                                            if (!isMax) {
                                                coroutine.launch {
                                                    setPosition(
                                                        sliderHeight.toPx() / 2 - dotSize.toPx() / 2,
                                                        -positionY
                                                    )
                                                }
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
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.slider_arrows),
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.onTertiary
                )
            }
        }
    }
}

@Composable
fun ZoomSlider(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    enabled: Boolean = false,
    hapticFeedbackEnabled: Boolean,
    controller: PTZController?
) {
    SliderBox(
        modifier = modifier,
        setPosition = { maxPos, posY ->
            mainViewModel.setZoomIntensity(maxPos, posY)
        },
        updateStatus = {
            if(controller != null)
                mainViewModel.updateZoomLevel()
        },
        hapticFeedbackEnabled = hapticFeedbackEnabled,
        enabled = enabled
    )
}

@Composable
fun FocusSlider(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    enabled: Boolean = false,
    hapticFeedbackEnabled: Boolean
) {
    SliderBox(
        modifier = modifier,
        setPosition = { maxPos, posY ->
            mainViewModel.setFocusIntensity(maxPos, posY)
        },
        hapticFeedbackEnabled = hapticFeedbackEnabled,
        enabled = enabled
    )
}