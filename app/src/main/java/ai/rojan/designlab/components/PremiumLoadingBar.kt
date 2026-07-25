package ai.rojan.designlab.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.theme.RojanLoadingGlowEnd
import ai.rojan.designlab.ui.theme.RojanLoadingGlowMid
import ai.rojan.designlab.ui.theme.RojanLoadingGlowStart
import ai.rojan.designlab.ui.theme.RojanLoadingTrack

/**
 * ROJAN DesignLab
 * Premium Loading Bar
 */

@Composable
fun PremiumLoadingBar(
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
    trackColor: Color = RojanLoadingTrack,
    glowColors: List<Color> = listOf(
        Color.Transparent,
        RojanLoadingGlowStart,
        RojanLoadingGlowMid,
        RojanLoadingGlowEnd,
        Color.Transparent
    ),
    durationMillis: Int = 2500,
) {

    val infiniteTransition =
        rememberInfiniteTransition(label = "loading")

    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {

        val glowWidth = size.width * 0.4f
        val glowCenter = size.width * sweepProgress

        drawRect(
            brush = Brush.linearGradient(
                colors = glowColors,
                start = Offset(
                    glowCenter - glowWidth,
                    0f
                ),
                end = Offset(
                    glowCenter + glowWidth,
                    0f
                )
            )
        )

    }
}

@Preview(showBackground = true, backgroundColor = 0xFF3A2E2A)
@Composable
private fun PremiumLoadingBarPreview() {
    PremiumLoadingBar(modifier = Modifier.padding(24.dp))
}

