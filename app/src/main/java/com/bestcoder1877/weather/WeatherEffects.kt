package com.bestcoder1877.weather

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

@Composable
fun ClearEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "clear")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(3000)),
        label = "clearShimmer"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        repeat(4) { i ->
            val x = width * 0.25f + i * width * 0.18f
            val y = height * 0.75f
            val glow = 0.3f + 0.2f * sin((shimmer * 10 + i).toDouble()).toFloat()
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = glow),
                radius = 120f + i * 30f,
                center = Offset(x + shimmer * 50f, y)
            )
        }
    }
}

@Composable
fun PartlyCloudyEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(20000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "cloudOffset"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        repeat(12) { i ->
            val x = (offset * width * 0.15f + i * width * 0.12f) % width
            val y = height * 0.7f + (i % 3) * height * 0.08f
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = 120f,
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = 90f,
                center = Offset(x + 60f, y + 40f)
            )
        }
    }
}

@Composable
fun OvercastEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "overcast")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "overcastOffset"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        repeat(8) { i ->
            val x = (offset * width * 0.1f + i * width * 0.15f) % width
            val y = height * 0.65f + (i % 3) * height * 0.1f
            drawCircle(
                color = Color.Gray.copy(alpha = 0.15f),
                radius = 100f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun FogEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "fog")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "fogOffset"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val alpha = 0.15f + 0.1f * sin((offset * 10).toDouble()).toFloat()
        drawRect(
            color = Color.White.copy(alpha = alpha),
            topLeft = Offset(0f, height * 0.5f),
            size = Size(width, height * 0.5f)
        )
    }
}

@Composable
fun RainEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = androidx.compose.animation.core.LinearEasing)),
        label = "rainOffset"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val dropCount = 100
        val visibleHeight = height * 0.4f

        repeat(dropCount) { i ->
            val seed = i * 12345L
            val x = ((seed % 1000) / 1000f) * width
            val yOffset = (offsetY * visibleHeight) % visibleHeight
            val y = height * 0.6f + (yOffset + visibleHeight * (i % 100) / 100f) % visibleHeight
            val length = 20f + ((i % 15) * 1f)

            drawLine(
                color = Color.White.copy(alpha = 1f),
                start = Offset(x, y),
                end = Offset(x - 3, (y + length) % height),
                strokeWidth = 3f
            )
        }
    }
}

@Composable
fun SnowEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "snow")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "snowOffset"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val flakeCount = 100
        val visibleHeight = height * 0.4f

        repeat(flakeCount) { i ->
            val seed = i * 98765L
            val xBase = ((seed % 1000) / 1000f) * width
            val sway = sin((offsetY * 10 + i * 0.1f).toDouble()).toFloat() * 25f
            val x = xBase + sway
            val yOffset = (offsetY * visibleHeight) % visibleHeight
            val y = height * 0.6f + (yOffset + visibleHeight * (i % 50) / 50f) % visibleHeight
            val size = 3f + ((i % 4) * 1.5f)

            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = size,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun StormEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "lightning")
    val flash by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(5000)),
        label = "lightning"
    )

    Box(modifier = modifier) {
        RainEffect(modifier = Modifier.fillMaxSize())
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (flash > 0.98f) {
                drawRect(color = Color.White.copy(alpha = 0.2f), size = size)
            }
        }
    }
}

@Composable
fun WeatherEffects(weatherCode: Int, modifier: Modifier = Modifier, precipitation: Double = 0.0) {
    val isRaining = precipitation >= 20.0
    when (weatherCode) {
        0 -> ClearEffect(modifier = modifier)
        1, 2 -> PartlyCloudyEffect(modifier = modifier)
        3 -> OvercastEffect(modifier = modifier)
        45, 48 -> FogEffect(modifier = modifier)
        in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82) ->
            if (isRaining) RainEffect(modifier = modifier) else OvercastEffect(modifier = modifier)
        in listOf(71, 73, 75, 77, 85, 86) -> SnowEffect(modifier = modifier)
        in listOf(95, 96, 97, 99) ->
            if (isRaining) StormEffect(modifier = modifier) else OvercastEffect(modifier = modifier)
    }
}
