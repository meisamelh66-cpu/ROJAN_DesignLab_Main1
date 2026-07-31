package ai.rojan.designlab.ui.components.glass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

import ai.rojan.designlab.ui.theme.CustomerPalette
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanShadows

/**
 * ROJAN Glass System base primitive (Design Token Spec v1.0, Section 6).
 * History: migrated off raw literals onto tokens, gained shadow/highlight
 * ownership, went through several fill/border alpha tuning passes, then
 * the Premium Glass Border Design Update (6-stop metallic sweep via
 * [premiumMetallicBorder]) — see git history for the full account.
 *
 * Shared Premium Glass Design System refactor: this is now a thin,
 * palette-bound wrapper around [PremiumGlassSurface] (the canonical
 * mechanic, Manager is its reference implementation) rather than its own
 * implementation — [CustomerPalette] is bound here so this component's
 * ~4 remaining real call sites (most of Customer migrated onto
 * [ai.rojan.designlab.screens.customer.hometheme.HomeGlassSurface] since
 * this doc was last accurate) keep working unchanged. Fill alpha moved
 * from this file's own historical 0.46f/0.18f to the canonical
 * 0.14f/0.06f as part of that unification — a real, deliberate opacity
 * change for those call sites, not a no-op default.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    glassAlpha: Float = PremiumGlassTheme.FillAlpha,
    glassSecondaryAlpha: Float = PremiumGlassTheme.FillSecondaryAlpha,
    borderAlpha: Float = PremiumGlassTheme.BorderAlpha,
    borderSecondaryAlpha: Float = PremiumGlassTheme.BorderSecondaryAlpha,
    elevation: Dp = RojanShadows.FloatingElevation,
    showHighlight: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalRojanPalette provides CustomerPalette) {
        PremiumGlassSurface(
            modifier = modifier,
            shape = shape,
            fillAlpha = glassAlpha,
            fillSecondaryAlpha = glassSecondaryAlpha,
            borderAlpha = borderAlpha,
            borderSecondaryAlpha = borderSecondaryAlpha,
            elevation = elevation,
            showHighlight = showHighlight,
            content = content,
        )
    }
}