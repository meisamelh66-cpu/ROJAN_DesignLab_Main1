package ai.rojan.designlab.ui.components.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.components.effects.RojanAmbientGlow
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes

/**
 * ROJAN AI's single "featured" circular icon container — design-system
 * refinement, Phase 4 (Unified Premium Icon Container).
 *
 * Before this pass, Manager owned the only such container
 * ([ai.rojan.designlab.manager.components.ManagerIconContainer]): a soft
 * [RojanAmbientGlow] behind a circular glass surface (accent-tinted
 * gradient fill, accent→gold gradient rim), hardcoded to `ManagerColors`.
 * This is that same mechanic, generalized to any [LocalRojanPalette] and
 * shared by every app. [RojanIconContainer] (the plain icon primitive)
 * is reused for the glyph itself — this component only adds the
 * *container* (glow + circular surface) around it, so icon rendering
 * itself is never duplicated.
 *
 * **Why this isn't a thin [ai.rojan.designlab.ui.components.glass.PremiumGlassSurface]
 * wrapper**, even though that's the shared glass mechanic everywhere
 * else: `PremiumGlassSurface`'s fill is fixed to *one* palette-level tint
 * (`LocalRojanPalette.current.fillTint`) — it has no per-call accent
 * parameter. Manager's reference design deliberately alternates
 * teal-tinted and gold-tinted icon circles across one screen (e.g. the
 * KPI row) — routing through `PremiumGlassSurface` as-is would flatten
 * every icon to the same app-level tint (Manager's `fillTint` is `null`
 * = white), erasing that real, intentional distinction. [RojanAmbientGlow]
 * (already shared, not duplicated) provides the glow; the fill/border are
 * built directly here so [accentColor] can vary per call, same as before
 * unification — only now driven by [LocalRojanPalette] instead of
 * `ManagerColors` literals, and shaped by [RojanShapes.Circle]
 * (→ [ai.rojan.designlab.ui.theme.RojanRadius.Circle]) instead of a raw
 * `CircleShape`.
 *
 * [style] mirrors [ai.rojan.designlab.ui.components.buttons.RojanButtonStyle]'s
 * Glass/Gradient duality:
 * - [RojanIconContainerStyle.Glass] — translucent accent wash + gradient
 *   rim. Manager's exact pre-unification look, byte-identical for its own
 *   two accents (Turquoise/Gold) except the fill's second gradient stop,
 *   which generalizes `ManagerColors.BaseSecondary` to
 *   `palette.shadowAmbient` (`ManagerColors.BaseDeep`) — both are
 *   near-black dark-teal tones at 40% alpha over the same dark canvas;
 *   the difference is not perceptible in practice, and adding a
 *   dedicated palette field for one shade this close was judged not
 *   worth the extra contract surface (see [ai.rojan.designlab.ui.theme.RojanAppPalette]'s
 *   own "no speculative fields" rule).
 * - [RojanIconContainerStyle.Gradient] — opaque accent-to-accent fill,
 *   for a bolder featured icon. New this pass; no current call site uses
 *   it (same "built for completeness, not yet exercised" status as
 *   [ai.rojan.designlab.ui.components.buttons.RojanButtonStyle.Outline]).
 */
enum class RojanIconContainerStyle {
    Glass,
    Gradient,
}

object PremiumIconContainerDefaults {
    /** Manager's original `ManagerIconTheme.ContainerSize` — reuses the existing 48dp touch-target token rather than a new one. */
    val ContainerSize: Dp = RojanDimens.MinTouchTarget
    const val GlowSizeMultiplier: Float = 1.6f
}

@Composable
fun PremiumIconContainer(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: RojanIconSize = RojanIconSize.Medium,
    containerSize: Dp = PremiumIconContainerDefaults.ContainerSize,
    style: RojanIconContainerStyle = RojanIconContainerStyle.Glass,
    /** Palette-driven by default — [LocalRojanPalette.current.shadowSpot] (Manager: Turquoise, Customer: Glow, Reception: Gold). Freely overridable per call for a screen that alternates accents across several icons (e.g. Manager's KPI row). */
    accentColor: Color = LocalRojanPalette.current.shadowSpot,
    /** The glyph's own tint — defaults to [accentColor]; override for a lighter/contrasting variant. */
    iconTint: Color = accentColor,
    showGlow: Boolean = true,
) {
    val palette = LocalRojanPalette.current

    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center,
    ) {
        if (showGlow) {
            RojanAmbientGlow(
                modifier = Modifier.size(containerSize * PremiumIconContainerDefaults.GlowSizeMultiplier),
                color = accentColor,
                alpha = 0.35f,
            )
        }

        Box(
            modifier = Modifier
                .size(containerSize)
                .shadow(
                    elevation = RojanShadows.SoftElevation,
                    shape = RojanShapes.Circle,
                    ambientColor = accentColor.copy(alpha = 0.35f),
                    spotColor = palette.shadowAmbient.copy(alpha = 0.6f),
                )
                .background(
                    brush = when (style) {
                        RojanIconContainerStyle.Glass -> Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.30f),
                                palette.shadowAmbient.copy(alpha = 0.40f),
                            ),
                        )

                        RojanIconContainerStyle.Gradient -> Brush.linearGradient(
                            colors = listOf(accentColor, palette.shadowAmbient),
                        )
                    },
                    shape = RojanShapes.Circle,
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.65f),
                            palette.textAccent.copy(alpha = 0.40f),
                        ),
                    ),
                    shape = RojanShapes.Circle,
                ),
            contentAlignment = Alignment.Center,
        ) {
            RojanIconContainer(
                imageVector = imageVector,
                contentDescription = contentDescription,
                size = size,
                tint = iconTint,
            )
        }
    }
}
