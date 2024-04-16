package it.mobile.bisax.ptzvision.ui.console.blocks

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import it.mobile.bisax.ptzvision.ui.console.MainViewModel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.ui.unit.min as minDp

@Composable
fun JoyStick(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    moved: (x: Float, y: Float) -> Unit = { _, _ -> },
    enabled: Boolean = true
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(15.dp)
    ) {
        val padSize = minDp(maxWidth, maxHeight)
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
            val dotSize: Dp = padSize / 3.5f
            val maxRadius = with(LocalDensity.current) { (padSize / 2).toPx() }
            val centerX = with(LocalDensity.current) { ((padSize - dotSize) / 2).toPx() }
            val centerY = with(LocalDensity.current) { ((padSize - dotSize) / 2).toPx() }

            var offsetX by remember { mutableFloatStateOf(centerX) }
            var offsetY by remember { mutableFloatStateOf(centerY) }

            var radius by remember { mutableFloatStateOf(0f) }
            var theta by remember { mutableFloatStateOf(0f) }

            var positionX by remember { mutableFloatStateOf(0f) }
            var positionY by remember { mutableFloatStateOf(0f) }


            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (positionX + centerX).roundToInt(),
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
                                        offsetX = centerX
                                        offsetY = centerY
                                        radius = 0f
                                        theta = 0f
                                        positionX = 0f
                                        positionY = 0f

                                        mainViewModel.setPTIntensity()
                                    }) { pointerInputChange: PointerInputChange, offset: Offset ->
                                        val x = offsetX + offset.x - centerX
                                        val y = offsetY + offset.y - centerY

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

                                        if (radius > maxRadius) {
                                            polarToCartesian(maxRadius, theta)
                                        } else {
                                            polarToCartesian(radius, theta)
                                        }.apply {
                                            positionX = first
                                            positionY = second

                                            mainViewModel.setPTIntensity(
                                                padSize.toPx() / 2,
                                                min(radius, maxRadius),
                                                theta
                                            )
                                        }
                                    }
                                }
                                .onGloballyPositioned { coordinates ->
                                    moved(
                                        (coordinates.positionInParent().x - centerX) / maxRadius,
                                        -(coordinates.positionInParent().y - centerY) / maxRadius
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

private fun polarToCartesian(radius: Float, theta: Float): Pair<Float, Float> =
    Pair(radius * cos(theta), radius * sin(theta))

@Composable
fun SliderBox(
    modifier: Modifier = Modifier,
    moved: (y: Float) -> Unit = { _ -> },
    setPosition: (maxPos:Float, posY: Float) -> Unit,
    enabled: Boolean = false
){
    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        val sliderHeight = maxHeight
        val sliderWidth = maxHeight*0.3f
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

                                        if (abs(radius) > maxYOffset) {
                                            polarToCartesian(sign(radius) * maxYOffset, theta)
                                        } else {
                                            polarToCartesian(radius, theta)
                                        }.apply {
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