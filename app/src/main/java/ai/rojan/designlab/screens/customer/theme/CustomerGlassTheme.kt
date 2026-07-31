package ai.rojan.designlab.screens.customer.theme

import ai.rojan.designlab.ui.components.glass.premiumMetallicBorder
import ai.rojan.designlab.ui.theme.RojanShadows
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.text.Text

/**
 * ROJAN Customer glass tuning — pairs with [CustomerGlassSurface].
 * Fill stays close to the shared `GlassSurface` default; the real
 * differentiators are the Rose Gold edge light and the ambient glow/
 * dual-shadow depth below, per explicit feedback that a single flat
 * translucent fill + one shadow reads as "a white box," not elevated
 * glass — "every major component must appear visually elevated above
 * the background... cards must not blend into the page."
 */
object CustomerGlassTheme {
    // Visual Intensity Correction v2: the first pass (0.50f/0.22f fill,
    // 0.55f/0.20f border) rendered nearly indistinguishable from the page
    // — translucent *white* glass on top of an already near-white/pale-
    // pink page has almost no brightness contrast to separate the two,
    // regardless of how much shadow/border logic sits underneath it.
    // Pushed hard, disclosed as a real visual jump, not a tuning nudge:
    // fill opaque enough to read as a genuinely brighter "cut out" card
    // against the tinted page, border a fully visible Rose Gold ring
    // rather than a near-transparent hairline.
    const val FillAlpha = 0.82f
    const val FillSecondaryAlpha = 0.42f

    // Premium Glass Border Design Update: retuned again per explicit
    // follow-up ("too weak... barely visible... not neon" — distinct
    // from oversaturated flatness) — near-opaque 1f/0.9f, paired with
    // the shared size-aware metallic border (see premiumMetallicBorder).
    const val BorderAlpha = 1f
    const val BorderSecondaryAlpha = 0.9f
}

/**
 * ROJAN Customer feminine luxury glass surface. Layered depth, same
 * proven technique this codebase already uses for elevated cards
 * ([ai.rojan.designlab.ui.components.cards.RojanHomeCard]/
 * [ai.rojan.designlab.components.hero.HeroBookingCard]: a blurred
 * ambient-glow layer *behind* a crisp glass surface, not a single flat
 * fill) — reused here rather than reinvented:
 *
 * 1. **Ambient glow layer** (behind, blurred, gradient-fill so it
 *    doesn't wash into a hard-edged block) — the "floating above the
 *    background" depth separation.
 * 2. **Dual shadow** — a soft, larger, Rose Gold-tinted glow shadow
 *    plus a tighter, darker, neutral contact shadow underneath it —
 *    "multiple shadow layers," not one flat drop-shadow.
 * 3. **Translucent glass fill** — unchanged in spirit from the shared
 *    `GlassSurface`, slightly more opaque for legibility.
 * 4. **Edge lighting** — a visibly bright Rose Gold → White gradient
 *    border (not a near-invisible hairline).
 * 5. **Two inner highlights** — a brighter one top-start, a quieter
 *    Rose Gold one bottom-end, so the glass reads as catching light
 *    from more than one direction rather than one flat corner glint.
 *
 * Foundation only — not wired into any existing screen's *cards* yet
 * (Splash, the only screen currently using the Customer theme, has no
 * card content of its own). The shared `GlassSurface` stays exactly as
 * it is; this doesn't replace it.
 */
@Composable
fun CustomerGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    fillAlpha: Float = CustomerGlassTheme.FillAlpha,
    fillSecondaryAlpha: Float = CustomerGlassTheme.FillSecondaryAlpha,
    borderAlpha: Float = CustomerGlassTheme.BorderAlpha,
    borderSecondaryAlpha: Float = CustomerGlassTheme.BorderSecondaryAlpha,
    elevation: Dp = RojanShadows.FloatingElevation,
    showAmbientGlow: Boolean = true,
    showHighlight: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box {
        // Layer 0: blurred ambient glow behind the glass — the actual
        // "depth separation from the page" effect. Gradient fill (not
        // a flat color) so the blur softens naturally instead of
        // washing into a hard-edged block.
        if (showAmbientGlow) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(28.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                CustomerColors.RoseGold.copy(alpha = 0.48f),
                                CustomerColors.BlushPink.copy(alpha = 0.40f),
                            ),
                        ),
                        shape = shape,
                    ),
            )
        }

        // Layer 1+2: dual shadow (soft colored glow + crisp contact
        // shadow), fill, and edge-light border — the crisp "floating
        // card" itself. Carries the caller's `modifier` (e.g.
        // `fillMaxWidth()`) so it — not the ambient glow — determines
        // this composable's resolved size.
        BoxWithConstraints(
            modifier = modifier
                // Glow shadow — soft, wide, Rose Gold — the "floating"
                // read from a distance.
                .shadow(
                    elevation = elevation + 18.dp,
                    shape = shape,
                    ambientColor = CustomerColors.RoseGold.copy(alpha = 0.38f),
                    spotColor = CustomerColors.RoseGold.copy(alpha = 0.30f),
                )
                // Real dark contact shadow — genuinely darker, not just a
                // colored tint, so the card visibly *lifts off* the page
                // rather than just glowing on top of it.
                .shadow(
                    elevation = elevation + 6.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.22f),
                    spotColor = Color.Black.copy(alpha = 0.18f),
                )
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    ambientColor = RojanShadows.PremiumShadow.copy(alpha = 0.22f),
                    spotColor = RojanShadows.PremiumShadow.copy(alpha = 0.16f),
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = fillAlpha),
                            CustomerColors.PearlPink.copy(alpha = fillSecondaryAlpha),
                        ),
                    ),
                    shape = shape,
                )
                // Premium Glass Border Design Update: shared size-aware
                // metallic border (was a single-hue Rose Gold ring, then
                // a fixed-pixel-space gradient that read flat on most
                // card sizes — see premiumMetallicBorder).
                .premiumMetallicBorder(
                    shape = shape,
                    baseAlpha = borderAlpha,
                    secondaryAlpha = borderSecondaryAlpha,
                ),
        ) {
            if (showHighlight) {
                val density = LocalDensity.current
                val highlightRadiusPx = with(density) {
                    (maxWidth.coerceAtLeast(maxHeight) * 0.4f).toPx()
                }

                // Brighter highlight, top-start.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.75f),
                                    CustomerColors.BlushPink.copy(alpha = 0.30f),
                                    Color.Transparent,
                                ),
                                center = Offset.Zero,
                                radius = highlightRadiusPx.coerceAtLeast(1f),
                            ),
                            shape = shape,
                        ),
                )

                // Quieter Rose Gold highlight, bottom-end — glass
                // catching light from a second direction.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CustomerColors.RoseGold.copy(alpha = 0.38f),
                                    Color.Transparent,
                                ),
                                center = Offset(
                                    with(density) { maxWidth.toPx() },
                                    with(density) { maxHeight.toPx() },
                                ),
                                radius = highlightRadiusPx.coerceAtLeast(1f) * 0.7f,
                            ),
                            shape = shape,
                        ),
                )
            }

            content()
        }
    }
}

@Preview(showBackground = true, widthDp = 220, heightDp = 160)
@Composable
private fun CustomerGlassSurfacePreview() {
    Box(modifier = Modifier.padding(24.dp)) {
        CustomerGlassSurface(
            modifier = Modifier,
            shape = RojanShapes.Small,
        ) {
            Box(modifier = Modifier.padding(20.dp)) {
                Text("پیش‌نمایش", color = CustomerColors.TextPrimary)
            }
        }
    }
}
