package ai.rojan.designlab.ui.components.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

import ai.rojan.designlab.ui.components.glass.PremiumGlassSurface
import ai.rojan.designlab.ui.components.glass.PremiumGlassTheme
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes

/**
 * ROJAN AI's single card shell — design-system refinement, Phase 4
 * (Premium Card Shell System), **foundation only**: this component exists
 * and is tested, but migrates no existing screen yet (see the Phase 4
 * migration-plan report — every current card stays exactly as it renders
 * today until a separately-reviewed migration pass).
 *
 * Every variant renders through [PremiumGlassSurface] — the one shared
 * glass mechanic — so there is exactly one shadow/border/highlight
 * implementation behind all three; only [rojanCardVariantSpec]'s four
 * numbers (fill/border alpha, elevation) differ per [RojanCardVariant].
 * [shape] defaults to [RojanShapes.GlassCard] (→ [ai.rojan.designlab.ui.theme.RojanRadius.Card]);
 * no raw corner `.dp` anywhere in this file. Colors are never touched
 * directly — [PremiumGlassSurface] already resolves fill/border/highlight
 * from `LocalRojanPalette`, so a `PremiumCardShell` dropped into any of
 * the three apps automatically renders in that app's identity.
 *
 * [onClick] is optional: `null` (the default) renders a static
 * card — the majority of today's cards outside browse rows.
 */
enum class RojanCardVariant {
    /** Standard translucent glass card — the default for most content. Elevation: [RojanShadows.FloatingElevation] ("standard cards," per the app's shadow-by-intent rule). */
    GlassCard,

    /** Near-opaque — a high-fill-alpha glass surface rather than a hardcoded flat color (no palette field represents "app canvas base," and inventing one speculatively is against [ai.rojan.designlab.ui.theme.RojanAppPalette]'s own "no speculative fields" rule). Reads calmer/flatter than [GlassCard]. Elevation: [RojanShadows.SoftElevation] ("resting elements"). */
    SolidCard,

    /** Emphasized — for a card that should stand out (a hero moment, a featured/selected state). Elevation: [RojanShadows.PremiumElevation] ("hero-level and lifted elements"). */
    HighlightCard,
}

/** The four numbers that actually differ between [RojanCardVariant]s — the only per-variant surface area; everything else routes through the one [PremiumGlassSurface] call in [PremiumCardShell]. */
internal data class RojanCardVariantSpec(
    val fillAlpha: Float,
    val fillSecondaryAlpha: Float,
    val borderAlpha: Float,
    val elevation: Dp,
)

/** Pure — no [androidx.compose.runtime.Composable] dependency, so this mapping is directly unit-testable without a Compose UI test harness. */
internal fun rojanCardVariantSpec(variant: RojanCardVariant): RojanCardVariantSpec = when (variant) {
    RojanCardVariant.GlassCard -> RojanCardVariantSpec(
        fillAlpha = PremiumGlassTheme.FillAlpha,
        fillSecondaryAlpha = PremiumGlassTheme.FillSecondaryAlpha,
        borderAlpha = PremiumGlassTheme.BorderAlpha,
        elevation = RojanShadows.FloatingElevation,
    )

    RojanCardVariant.SolidCard -> RojanCardVariantSpec(
        fillAlpha = 0.85f,
        fillSecondaryAlpha = 0.65f,
        borderAlpha = PremiumGlassTheme.BorderAlpha * 0.6f,
        elevation = RojanShadows.SoftElevation,
    )

    RojanCardVariant.HighlightCard -> RojanCardVariantSpec(
        fillAlpha = PremiumGlassTheme.FillAlpha * 1.5f,
        fillSecondaryAlpha = PremiumGlassTheme.FillSecondaryAlpha * 1.5f,
        borderAlpha = PremiumGlassTheme.BorderAlpha,
        elevation = RojanShadows.PremiumElevation,
    )
}

@Composable
fun PremiumCardShell(
    modifier: Modifier = Modifier,
    variant: RojanCardVariant = RojanCardVariant.GlassCard,
    shape: Shape = RojanShapes.GlassCard,
    contentPadding: PaddingValues = PaddingValues(RojanDimens.SpaceMD),
    /** Pass-through to [PremiumGlassSurface] — a lighter border/glow treatment for small surfaces (chips, badges). */
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spec = rojanCardVariantSpec(variant)

    PremiumGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.rojanPressable(onClick = onClick) else it },
        shape = shape,
        fillAlpha = spec.fillAlpha,
        fillSecondaryAlpha = spec.fillSecondaryAlpha,
        borderAlpha = spec.borderAlpha,
        elevation = spec.elevation,
        compact = compact,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content,
        )
    }
}
