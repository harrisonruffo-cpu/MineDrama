package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DramaCrimsonBright
import kotlin.math.roundToInt

data class HeartEffect(
    val id: Long,
    val x: Float,
    val y: Float
)

@Composable
fun HeartBurstOverlay(
    heart: HeartEffect,
    onAnimationEnd: () -> Unit
) {
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(1f) }
    val offsetY = remember { Animatable(0f) }

    LaunchedEffect(heart.id) {
        scale.animateTo(
            targetValue = 1.4f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1.1f,
            animationSpec = tween(durationMillis = 150)
        )
        offsetY.animateTo(
            targetValue = -100f,
            animationSpec = tween(durationMillis = 400)
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300)
        )
        onAnimationEnd()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(heart.x.roundToInt() - 50, (heart.y + offsetY.value).roundToInt() - 50) }
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = DramaCrimsonBright,
            modifier = Modifier
                .size(100.dp)
                .scale(scale.value)
                .alpha(alpha.value)
        )
    }
}
