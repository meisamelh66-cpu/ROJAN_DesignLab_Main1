package ai.rojan.designlab.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Reusable frosted-glass floating orb.
 *
 * ROJAN DesignLab Component
 * Version: R001
 */
@Composable
fun GlassOrb(
    modifier: Modifier = Modifier,
    size: Dp = 90.dp,
    colors: List<Color>,
    floatDistance: Dp = 18.dp,
    durationMillis: Int = 4200,
    blurRadius: Dp = 1.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_float")

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_offset"
    )

    val yOffset = (floatOffset - 0.5f) * 2f * floatDistance.value

    Canvas(
        modifier = modifier
            .offset { IntOffset(0, yOffset.toInt()) }
            .size(size)
            .blur(blurRadius)
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = colors,
                center = center.copy(
                    x = center.x * 0.7f,
                    y = center.y * 0.7f
                )
            )
        )
    }
}

@Composable
fun FrostedGlassOrb(
    modifier: Modifier = Modifier,
    size: Dp = 90.dp,
    tint: Color = Color.White,
    alpha: Float = 0.35f,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                tint.copy(alpha = alpha)
            )
            .blur(2.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF8F6)
@Composable
private fun GlassOrbPreview() {
    Box(modifier = Modifier.size(160.dp)) {
        GlassOrb(
            colors = listOf(Color(0xFFD1B3FF), Color.Transparent),
            size = 100.dp,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF8F6)
@Composable
private fun FrostedGlassOrbPreview() {
    FrostedGlassOrb(
        size = 90.dp,
        tint = Color(0xFFFFC1D6),
        alpha = 0.45f,
    )
}

