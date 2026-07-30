package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.theme.RojanShadows
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/**
 * ROJAN Manager glass card tuning — pairs with [ManagerGlassSurface].
 * Frosted-glass alpha values distinct from the shared `GlassSurface`
 * default (which is a white-based fill meant for a warm-white
 * background) — these are tuned so a translucent surface still reads
 * as "frosted glass" floating above the dark [ManagerBackgroundTheme],
 * not a flat dark rectangle.
 */
object ManagerGlassTheme {
    const val FillAlpha = 0.14f
    const val FillSecondaryAlpha = 0.06f
    const val BorderAlpha = 0.55f
    const val BorderSecondaryAlpha = 0.25f
}

/**
 * ROJAN Manager dark glass surface — the [ai.rojan.designlab.ui.components.glass.GlassSurface]
 * counterpart for the dark luxury theme. Same layered technique
 * (crisp fill + gradient border + optional top-start highlight,
 * elevation shadow) as the shared `GlassSurface`, but that component
 * hardcodes a white-based fill/border with no color parameter to
 * retint — reusing it as-is for a dark theme would just look like hazy
 * white smudges, not "frosted dark glass with turquoise edge light."
 * This is a small, Manager-only sibling, not a fork of the shared
 * component (Customer/`GlassSurface.kt` are untouched).
 */
@Composable
fun ManagerGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    fillAlpha: Float = ManagerGlassTheme.FillAlpha,
    fillSecondaryAlpha: Float = ManagerGlassTheme.FillSecondaryAlpha,
    borderAlpha: Float = ManagerGlassTheme.BorderAlpha,
    borderSecondaryAlpha: Float = ManagerGlassTheme.BorderSecondaryAlpha,
    elevation: Dp = RojanShadows.FloatingElevation,
    showHighlight: Boolean = true,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = ManagerColors.BaseDeep.copy(alpha = 0.55f),
                spotColor = ManagerColors.Turquoise.copy(alpha = 0.25f),
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = fillAlpha),
                        Color.White.copy(alpha = fillSecondaryAlpha),
                    ),
                ),
                shape = shape,
            )
            .border(
                width = 0.6.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        ManagerColors.Turquoise.copy(alpha = borderAlpha),
                        ManagerColors.Gold.copy(alpha = borderSecondaryAlpha),
                    ),
                    start = Offset.Zero,
                    end = Offset(600f, 600f),
                ),
                shape = shape,
            ),
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
                                ManagerColors.TurquoiseLight.copy(alpha = 0.18f),
                                ManagerColors.TurquoiseLight.copy(alpha = 0.05f),
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
