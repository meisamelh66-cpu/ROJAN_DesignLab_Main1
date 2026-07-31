package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.components.glass.PremiumGlassSurface
import ai.rojan.designlab.ui.components.glass.PremiumGlassTheme
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.ManagerPalette
import ai.rojan.designlab.ui.theme.RojanShadows
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * ROJAN Manager glass card tuning — pairs with [ManagerGlassSurface].
 * Manager's values are the canonical values for the whole Shared Premium
 * Glass Design System (see [PremiumGlassTheme]) — Manager is the named
 * master reference, so this object now just re-exports those constants
 * under their original Manager-facing names for the handful of call
 * sites (`ManagerCalendarScreen.kt`, `ManagerBookingDateTimeScreen.kt`)
 * that reference `ManagerGlassTheme.FillAlpha`/`BorderAlpha` directly for
 * selected/unselected chip states.
 */
object ManagerGlassTheme {
    const val FillAlpha = PremiumGlassTheme.FillAlpha
    const val FillSecondaryAlpha = PremiumGlassTheme.FillSecondaryAlpha
    const val BorderAlpha = PremiumGlassTheme.BorderAlpha
    const val BorderSecondaryAlpha = PremiumGlassTheme.BorderSecondaryAlpha
}

/**
 * ROJAN Manager dark glass surface — thin, palette-bound wrapper around
 * the shared [PremiumGlassSurface] mechanic (Shared Premium Glass Design
 * System). Manager IS the mechanic's reference implementation, so this
 * only provides [ManagerPalette] and forwards every parameter; call sites
 * are unchanged from before this refactor.
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
    CompositionLocalProvider(LocalRojanPalette provides ManagerPalette) {
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
