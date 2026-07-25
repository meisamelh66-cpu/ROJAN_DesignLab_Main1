package ai.rojan.designlab.ui.background

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.R
import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanGlassWhite
import ai.rojan.designlab.ui.theme.RojanNavy

/**
 * Home Screen Production Pass, Task 1 (Background): the desaturation
 * matrix applied when [PremiumBackground]'s [softenForContent] is
 * `true`. A top-level `val` rather than allocated inline so it isn't
 * rebuilt every recomposition — it holds no per-call state. 0.82f is a
 * subtle ~18% saturation reduction, not a full desaturation — "keep the
 * luxury salon feeling" rules out anything closer to grayscale.
 */
private val softenedBackgroundColorFilter =
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.82f) })

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
 *
 * Home Screen Production Pass, Task 1: [softenForContent] is an opt-in
 * addition — default `false` keeps every existing call site (Booking,
 * Profile, dashboards, etc.) pixel-identical to before. When `true`
 * (Customer Home only, per that pass's explicit scope), the photo gains
 * a moderate blur and a mild saturation reduction, and a translucent
 * [RojanGlassWhite] wash sits between the photo/glow and [content] —
 * together this reduces how much the background competes with the
 * foreground glass cards without touching the approved photo asset
 * itself or any other screen.
 */
@Composable
fun PremiumBackground(
    modifier: Modifier = Modifier,
    softenForContent: Boolean = false,
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
            colorFilter = if (softenForContent) softenedBackgroundColorFilter else null,
            modifier = Modifier
                .fillMaxSize()
                .let { if (softenForContent) it.blur(20.dp) else it }
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
        )

        if (softenForContent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RojanGlassWhite.copy(alpha = 0.14f))
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
