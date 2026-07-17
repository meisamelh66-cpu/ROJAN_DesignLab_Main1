package ai.rojan.designlab.ui.background

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource

import ai.rojan.designlab.R
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanNavy

/**
 * ROJAN AI premium app background — Official Master Background v2.0
 * (Light Luxury Pastel).
 *
 * [R.drawable.bg_master_luxury_salon] is the final, approved canonical
 * photo (bright luxury pastel salon — lavender, pearl white, blush pink,
 * champagne, rose-gold; registered as immutable, not to be
 * regenerated/redesigned): centered, uniformly scaled via
 * [ContentScale.Crop] (no distortion, no stretching across screen
 * sizes). Unlike the earlier placeholder this replaced, this image has
 * no fade baked into its own edges — [RojanNavy] still sits underneath
 * as the base fill (visible only in any Crop overflow margin, not as an
 * intentional vignette), and the [RojanAIGlow] radial layer on top
 * remains the one deliberate code-level lighting effect over the photo.
 */
@Composable
fun PremiumBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {

    BoxWithConstraints(
        modifier = modifier
            .background(RojanNavy)
    ) {

        Image(
            painter = painterResource(id = R.drawable.bg_master_luxury_salon),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        val density = LocalDensity.current
        val glowRadiusPx = with(density) { (maxWidth.coerceAtLeast(maxHeight) * 0.9f).toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            RojanAIGlow.copy(alpha = 0.12f),
                            RojanAIGlow.copy(alpha = 0f)
                        ),
                        center = Offset.Zero,
                        radius = glowRadiusPx
                    )
                )
        ) {
            content()
        }
    }
}
