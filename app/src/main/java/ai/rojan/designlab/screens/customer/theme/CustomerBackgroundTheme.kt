package ai.rojan.designlab.screens.customer.theme

import ai.rojan.designlab.ui.components.effects.RojanAmbientGlow
import ai.rojan.designlab.ui.theme.RojanBackgroundGradient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * ROJAN Customer feminine luxury background — foundation only, not
 * wired into any existing screen's *structure* (only Splash's
 * background wrapper currently uses this; its own layout/logo/
 * animation are untouched). Doesn't replace `WarmBackground`/
 * `PremiumBackground` themselves.
 *
 * Layered depth, all built from the existing, proven [RojanAmbientGlow]
 * primitive (radial gradient fading to transparent — reused rather than
 * duplicating blur logic; an earlier flat-color-plus-`Modifier.blur()`
 * attempt washed out into hard-edged squares, since `blur()`'s default
 * edge treatment clips to the rectangular layout bounds and a flat
 * color has no gradient falloff for the blur to soften — `RojanAmbientGlow`
 * avoids both problems by design):
 *
 * 1. Warm white base.
 * 2. The existing [RojanBackgroundGradient] wash (pink/aqua/lavender),
 *    strengthened for "large soft gradient areas" rather than a bare
 *    tint.
 * 3. A large Rose Gold glow centered on-screen — "ambient glow behind
 *    the logo area," since Splash's logo sits screen-center.
 *    Deliberately the largest, most prominent glow (the AFTER effect
 *    "the logo must feel like it is placed inside a luxury glass
 *    environment").
 * 4. Soft Lavender (top-start) and Blush Pink (bottom-end) corner
 *    glows for overall depth.
 * 5. A subtle Aqua reflection (center-end).
 * 6. Two small floating glass-glow orbs for extra depth, placed at
 *    TopCenter/BottomEnd-inset — corners Splash's own existing
 *    `FrostedGlassOrb` decorations don't use, so this ambient layer
 *    reads as depth behind them rather than colliding with them.
 *
 * [dense] (default `true`) is exactly the above, unchanged — Splash's
 * only caller passes no argument, so it's pixel-identical to before.
 * `dense = false` (Customer Login V2 refinement) is a calmer variant
 * for content screens with real foreground layout (a form, in Auth's
 * case) rather than one centered hero logo: the six scattered glows
 * above read fine behind a single centered logo, but behind a form
 * they compete with it and with each other for attention — exactly
 * the "random glows" the Login V2 spec says to avoid. The calm variant
 * keeps the same wash (still carries the aqua/lavender reflection,
 * just at lower strength so it doesn't dominate a mostly-white content
 * screen) and reduces the glows to one dominant Rose Gold source and
 * one quiet same-family echo, both anchored top-start/bottom-end — a
 * single consistent light direction, matching the top-start-bright/
 * bottom-end-quiet highlight direction already used by
 * [CustomerGlassSurface] and [CustomerTextField].
 */
@Composable
fun CustomerBackgroundTheme(
    modifier: Modifier = Modifier,
    dense: Boolean = true,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(CustomerColors.WarmWhite),
    ) {
        // Large soft pink/lavender/aqua wash — same token as
        // WarmBackground. Full strength for the immersive (Splash)
        // variant; softened for the calm variant so a mostly-white
        // content screen doesn't read as tinted/busy end to end.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = RojanBackgroundGradient.map {
                            it.copy(alpha = if (dense) 0.38f else 0.34f)
                        },
                    ),
                ),
        )

        if (dense) {
            // Rose Gold ambient glow, centered — sits directly behind
            // Splash's logo (which is screen-center), the key "luxury
            // glass environment around the logo" effect.
            RojanAmbientGlow(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(340.dp),
                color = CustomerColors.RoseGold,
                alpha = 0.30f,
                blurRadius = 60.dp,
            )

            // Soft Lavender glow, top-start — large, quiet corner depth.
            RojanAmbientGlow(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-60).dp, y = (-60).dp)
                    .size(240.dp),
                color = CustomerColors.SoftLavender,
                alpha = 0.35f,
                blurRadius = 55.dp,
            )

            // Blush Pink glow, bottom-end — large, quiet corner depth.
            RojanAmbientGlow(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 60.dp, y = 60.dp)
                    .size(260.dp),
                color = CustomerColors.BlushPink,
                alpha = 0.32f,
                blurRadius = 55.dp,
            )

            // Subtle Aqua reflection, center-end.
            RojanAmbientGlow(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 40.dp)
                    .size(200.dp),
                color = CustomerColors.AquaMint,
                alpha = 0.28f,
                blurRadius = 45.dp,
            )

            // Two small floating glass-glow orbs — extra depth, placed
            // away from Splash's own existing orb corners.
            RojanAmbientGlow(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
                    .size(72.dp),
                color = CustomerColors.SoftRoseGold,
                alpha = 0.45f,
                blurRadius = 18.dp,
            )

            RojanAmbientGlow(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 320.dp, end = 20.dp)
                    .size(56.dp),
                color = CustomerColors.Rose,
                alpha = 0.40f,
                blurRadius = 16.dp,
            )
        } else {
            // Visual Intensity Correction v2: the first calm pass (0.24f/
            // 0.18f glow alpha) was barely perceptible against the wash
            // — "layered lighting" needs to actually read as light, not
            // a faint tint. Pushed hard and enlarged, still just the two
            // sources (one consistent top-start direction, one quiet
            // bottom-end echo), not more scattered glows.

            // Dominant Rose Gold source, top-start.
            RojanAmbientGlow(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-60).dp, y = (-80).dp)
                    .size(420.dp),
                color = CustomerColors.RoseGold,
                alpha = 0.55f,
                blurRadius = 90.dp,
            )

            // A second, tighter, brighter core inside the same source —
            // gives the glow a visible "hot center" rather than one flat
            // blurred disc.
            RojanAmbientGlow(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-20).dp, y = (-40).dp)
                    .size(220.dp),
                color = CustomerColors.SoftRoseGold,
                alpha = 0.60f,
                blurRadius = 55.dp,
            )

            // Quiet, same-family echo, bottom-end — secondary falloff
            // from the same light source, not a competing one.
            RojanAmbientGlow(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 70.dp, y = 90.dp)
                    .size(320.dp),
                color = CustomerColors.BlushPink,
                alpha = 0.42f,
                blurRadius = 85.dp,
            )

            // Soft vignette — a gentle warm darkening toward the far
            // (bottom-start) corner, opposite the main light source, so
            // the page reads as lit from one direction with real falloff
            // rather than a uniformly bright wash. Deliberately subtle:
            // this adds depth, not a visible dark patch.
            val density = LocalDensity.current
            val vignetteCenterPx = with(density) {
                Offset(maxWidth.toPx() * 0.15f, maxHeight.toPx() * 0.9f)
            }
            val vignetteRadiusPx = with(density) {
                (maxWidth.coerceAtLeast(maxHeight) * 1.1f).toPx()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                CustomerColors.TextPrimary.copy(alpha = 0.06f),
                            ),
                            center = vignetteCenterPx,
                            radius = vignetteRadiusPx.coerceAtLeast(1f),
                        ),
                    ),
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CustomerBackgroundThemePreview() {
    CustomerBackgroundTheme {}
}
