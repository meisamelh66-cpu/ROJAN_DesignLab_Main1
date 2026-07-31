package ai.rojan.designlab.ui.components.glass

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanPremiumBorderGold
import ai.rojan.designlab.ui.theme.RojanPremiumBorderRoseGold
import ai.rojan.designlab.ui.theme.RojanShadows

/**
 * Shared Premium Glass Design System — the canonical glass-surface
 * mechanic every ROJAN app renders through (Manager's [ai.rojan.designlab.manager.components.ManagerGlassSurface],
 * Customer's [ai.rojan.designlab.ui.components.glass.GlassSurface] and
 * [ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface] are now
 * thin wrappers around this). Extracted verbatim from Manager's
 * implementation (the named master reference) — fill alpha, shadow-stack
 * offsets, and highlight radius are fixed constants here, not per-call
 * parameters, since "glass depth"/"blur intensity"/"layering" must be
 * mechanically identical across apps; only tint colors vary, sourced from
 * [LocalRojanPalette].
 *
 * [fillAlpha]/[fillSecondaryAlpha]/[borderAlpha]/[borderSecondaryAlpha]
 * default to the canonical mechanic's own values but stay overridable —
 * not a loophole for per-app forking, but the same real need Manager's
 * original implementation already had: state-dependent chips (selected
 * vs. unselected day/time/specialist filters) and a couple of explicit
 * local design overrides need a different opacity, not a different
 * mechanic. Every real override in the codebase today only ever adjusts
 * these two numeric knobs, never the shadow/highlight/border structure
 * itself.
 */
object PremiumGlassTheme {
    const val FillAlpha = 0.14f
    const val FillSecondaryAlpha = 0.06f
    const val BorderAlpha = 1f
    const val BorderSecondaryAlpha = 0.9f
}

@Composable
fun PremiumGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    fillAlpha: Float = PremiumGlassTheme.FillAlpha,
    fillSecondaryAlpha: Float = PremiumGlassTheme.FillSecondaryAlpha,
    borderAlpha: Float = PremiumGlassTheme.BorderAlpha,
    borderSecondaryAlpha: Float = PremiumGlassTheme.BorderSecondaryAlpha,
    elevation: Dp = RojanShadows.FloatingElevation,
    showHighlight: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = LocalRojanPalette.current

    BoxWithConstraints(
        modifier = modifier
            // Soft gold-tinted glow shadow, layered behind the app's own
            // base shadow — the "premium luxury glow" halo, app-agnostic
            // (the metallic border's own identity, not a palette concern).
            .shadow(
                elevation = elevation + 16.dp,
                shape = shape,
                ambientColor = RojanPremiumBorderGold.copy(alpha = 0.36f),
                spotColor = RojanPremiumBorderRoseGold.copy(alpha = 0.30f),
            )
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = palette.shadowAmbient.copy(alpha = 0.55f),
                spotColor = palette.shadowSpot.copy(alpha = 0.25f),
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = fillAlpha),
                        (palette.fillTint ?: Color.White).copy(alpha = fillSecondaryAlpha),
                    ),
                ),
                shape = shape,
            )
            .premiumMetallicBorder(
                shape = shape,
                baseAlpha = borderAlpha,
                secondaryAlpha = borderSecondaryAlpha,
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
                                palette.highlightTint.copy(alpha = 0.18f),
                                palette.highlightTint.copy(alpha = 0.05f),
                                Color.Transparent,
                            ),
                            center = Offset.Zero,
                            radius = highlightRadiusPx.coerceAtLeast(1f),
                        ),
                        shape = shape,
                    ),
            )
        }

        content()
    }
}
