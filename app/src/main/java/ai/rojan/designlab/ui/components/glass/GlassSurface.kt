package ai.rojan.designlab.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

import ai.rojan.designlab.ui.theme.RojanGlassBorder
import ai.rojan.designlab.ui.theme.RojanGlassWhite
import ai.rojan.designlab.ui.theme.RojanShadows

/**
 * ROJAN Glass System base primitive (Design Token Spec v1.0, Section 6).
 *
 * Migrated (Component 1 finalization pass) from raw `Color.White` literals
 * to [RojanGlassWhite]/[RojanGlassBorder] — both tokens' base color is
 * already pure white, so `.copy(alpha = ...)` here produces pixel-identical
 * output to before; this is a token-sourcing fix, not a visual change.
 * The two-stop gradient (fill and border each fade between two alpha
 * values) is preserved exactly as it was, since collapsing it to a single
 * flat token value would have changed the actual rendered look.
 *
 * Selective merge addition (see checkpoint-selective-merge-launcher-glass):
 * fill/border opacity are optional parameters rather than fixed literals.
 * Defaults match the exact original hardcoded values, so this alone
 * changed nothing visually for any existing caller.
 *
 * Design System Foundation Refactor addition (this pass): [elevation] and
 * [showHighlight] are new. **Unlike the opacity parameters above, this is
 * not a no-op default** — it's a deliberate visual improvement, disclosed
 * as such rather than framed as risk-free. Audited before this change:
 * 13 of this component's 14 real call sites applied zero shadow at all
 * (only [ai.rojan.designlab.ui.components.hero.HeroBookingCard] added its
 * own external `.shadow()`), which is exactly why most cards read as flat.
 * Moving shadow ownership into this shared primitive, with a sensible
 * default ([RojanShadows.FloatingElevation] — the "standard elevated
 * card" tier already defined in the Shadow System, as opposed to the
 * Hero-only Premium tier), means all 13 previously-shadowless consumers
 * gain consistent depth automatically.
 *
 * [showHighlight] adds a small, low-opacity radial highlight in the
 * upper-left — a conventional "light hitting glass" cue. The highlight's
 * radius is computed from this component's own measured size via
 * [BoxWithConstraints] (35% of the larger dimension) rather than a fixed
 * pixel value — a fixed radius would look proportionally correct on a
 * Hero-sized card and wildly oversized on a small chip, exactly the same
 * mistake already caught and fixed once before in
 * [ai.rojan.designlab.ui.background.PremiumBackground]'s glow layer.
 * Defaults to `true` since at this proportional sizing it stays subtle
 * across every card size actually in use (chips through Hero), but is a
 * real off-switch, not a parameter for its own sake.
 * ROJAN AI Customer UI Final Visual Refinement addition, corrected per
 * explicit follow-up: [glassAlpha] reduced ~10% from its original
 * 0.32f (proportional reduction, not an absolute 8%–12% target) —
 * 0.32 * 0.90 = 0.288, rounded to 0.29. [glassSecondaryAlpha] reduced
 * by the same ~10% from its original 0.10f — 0.10 * 0.90 = 0.09. Both
 * values reduced by the identical percentage, preserving their
 * original ratio to each other exactly. This keeps the luxury frosted-
 * glass character (depth, readability, premium feel) rather than
 * approaching near-transparency, per "the previous adjustment was
 * interpreted incorrectly... do not make glass nearly transparent."
 *
 * Global Design System Refinement (Phase 1): [glassAlpha]/[glassSecondaryAlpha]
 * increased again — "Current cards are too transparent... Increase
 * frost effect, glass tint, background blur. Reduce transparency." —
 * 0.29f -> 0.38f (main) and 0.09f -> 0.12f (secondary), same ratio
 * preserved as every prior pass. [borderAlpha]/[borderSecondaryAlpha]
 * brought down to the explicit "8-12% opacity... very subtle...
 * avoid strong outlines" target (previously 0.65f/0.15f, both
 * genuinely too strong for that description) — 0.10f/0.03f. A soft,
 * explicit shadow color (derived from [RojanShadows.PremiumShadow],
 * which existed as a token but was never actually wired into any real
 * `.shadow()` call until now) replaces Compose's default sharper
 * system shadow, for "very soft shadow... premium floating effect."
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    glassAlpha: Float = 0.38f,
    glassSecondaryAlpha: Float = 0.12f,
    borderAlpha: Float = 0.10f,
    borderSecondaryAlpha: Float = 0.03f,
    elevation: Dp = RojanShadows.FloatingElevation,
    showHighlight: Boolean = true,
    content: @Composable () -> Unit
) {

    BoxWithConstraints(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = RojanShadows.PremiumShadow.copy(alpha = 0.20f),
                spotColor = RojanShadows.PremiumShadow.copy(alpha = 0.20f),
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        RojanGlassWhite.copy(alpha = glassAlpha),
                        RojanGlassWhite.copy(alpha = glassSecondaryAlpha)
                    )
                ),
                shape = shape
            )
            .border(
                width = Dp.Hairline,
                brush = Brush.linearGradient(
                    colors = listOf(
                        RojanGlassBorder.copy(alpha = borderAlpha),
                        RojanGlassBorder.copy(alpha = borderSecondaryAlpha)
                    )
                ),
                shape = shape
            )
    ) {
        if (showHighlight) {
            val density = LocalDensity.current
            val highlightRadiusPx = with(density) {
                (maxWidth.coerceAtLeast(maxHeight) * 0.35f).toPx()
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0f)
                            ),
                            center = Offset.Zero,
                            radius = highlightRadiusPx.coerceAtLeast(1f)
                        ),
                        shape = shape
                    )
            )
        }

        content()
    }
}
