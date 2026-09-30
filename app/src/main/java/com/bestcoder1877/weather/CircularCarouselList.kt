package com.bestcoder1877.weather

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CircularCarouselList(
    lazyListState: LazyListState,
    items: List<String>,
    totalItems: Int,
    startIndex: Int,
    itemSize: Dp,
    visualItemSize: Dp,
    radiusPx: Float,
    density: Density,
    fontFamily: FontFamily,
    onSelectedHour: (Int) -> Unit
) {
    LazyRow(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        flingBehavior = rememberSnapFlingBehavior(lazyListState)
    ) {
        items(count = totalItems, key = { it }) { index ->
            val actualIndex = (index - startIndex).mod(items.size)
            CarouselItem(
                text = items[actualIndex],
                lazyListState = lazyListState,
                itemSize = itemSize,
                visualItemSize = visualItemSize,
                radiusPx = radiusPx,
                density = density,
                fontFamily = fontFamily,
                actualIndex = actualIndex,
                onSelectedHour = onSelectedHour
            )
        }
    }
}

@Composable
private fun CarouselItem(
    text: String,
    lazyListState: LazyListState,
    itemSize: Dp,
    visualItemSize: Dp,
    radiusPx: Float,
    density: Density,
    fontFamily: FontFamily,
    actualIndex: Int,
    onSelectedHour: (Int) -> Unit
) {
    var itemOffset by remember { mutableFloatStateOf(0f) }
    var itemWidth by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .size(itemSize)
            .onPlaced { coordinates ->
                itemOffset = coordinates.positionInParent().x
                itemWidth = coordinates.size.width.toFloat()
            }
            .graphicsLayer {
                val viewportWidth = lazyListState.layoutInfo.viewportSize.width.toFloat()
                if (viewportWidth > 0 && itemWidth > 0) {
                    val viewportCenter = viewportWidth / 2f
                    val itemCenter = itemOffset + itemWidth / 2f
                    val distance = itemCenter - viewportCenter
                    val indexFromCenter = distance / itemWidth
                    val angleStep = itemWidth / radiusPx
                    val angle = indexFromCenter * angleStep
                    val circleX = radiusPx * sin(angle)
                    val circleY = radiusPx * cos(angle)
                    
                    translationX = circleX - distance
                    translationY = -(radiusPx - circleY)
                }
                scaleX = visualItemSize / itemSize
                scaleY = visualItemSize / itemSize
                alpha = 1f
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 16.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        )
    }

    LaunchedEffect(itemOffset, itemWidth, lazyListState, actualIndex) {
        val viewportWidth = lazyListState.layoutInfo.viewportSize.width.toFloat()
        if (viewportWidth > 0 && itemWidth > 0) {
            val viewportCenter = viewportWidth / 2f
            val itemCenter = itemOffset + itemWidth / 2f
            val distance = abs(itemCenter - viewportCenter)
            if (distance <= itemWidth / 2f) {
                onSelectedHour(actualIndex)
            }
        }
    }
}
