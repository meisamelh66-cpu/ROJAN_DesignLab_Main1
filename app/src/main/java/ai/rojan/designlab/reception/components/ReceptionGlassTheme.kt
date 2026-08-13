package ai.rojan.designlab.reception.components

import ai.rojan.designlab.ui.components.glass.PremiumGlassSurface
import ai.rojan.designlab.ui.components.glass.PremiumGlassTheme
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanShadows
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * ROJAN Reception glass surface — thin, palette-bound wrapper around the
 * shared [PremiumGlassSurface] mechanic (Shared Premium Glass Design
 * System; Manager is the mechanic's reference implementation, see
 * `ManagerGlassTheme.kt`). Same pattern every app uses, only [ReceptionPalette]
 * differs — zero new glass mechanic, per
 * ROJAN_Reception_Implementation_Plan_v1.md, Phase 0.
 */
@Composable
fun ReceptionGlassSurface(
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
    CompositionLocalProvider(LocalRojanPalette provides ReceptionPalette) {
        PremiumGlassSurface(
            modifier = modifier,
            shape = shape,
            fillAlpha = fillAlpha,
            fillSecondaryAlpha = fillSecondaryAlpha,
            borderAlpha = borderAlpha,
            borderSecondaryAlpha = borderSecondaryAlpha,
            elevation = elevation,
            showHighlight = showHighlight,
            content = content,
        )
    }
}
