package ai.rojan.designlab.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.screens.customer.hometheme.HomeColors

/**
 * Shared Premium Glass Design System — the one thing allowed to differ
 * between ROJAN apps (Manager, Customer, and future Specialist/Reception/
 * Accountant/Inventory apps) is this palette; every rendering mechanic that
 * consumes it ([ai.rojan.designlab.ui.components.glass.PremiumGlassSurface]
 * first, more to follow) is otherwise identical across apps.
 *
 * Fields are limited to what the glass engine actually consumes today —
 * no speculative fields for icon accents / CTA gradients that later phases
 * will need; those get added when those phases are built.
 */
data class RojanAppPalette(
    val name: String,
    /** Second fill-gradient stop's tint; null means pure white (Manager's own look). */
    val fillTint: Color? = null,
    val shadowAmbient: Color,
    val shadowSpot: Color,
    val highlightTint: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textAccent: Color,
)

/** No default — a screen rendered without a provided palette should fail loudly, not render mystery colors. */
val LocalRojanPalette = staticCompositionLocalOf<RojanAppPalette> {
    error("No RojanAppPalette provided — wrap the app root in CompositionLocalProvider(LocalRojanPalette provides ...)")
}

val ManagerPalette = RojanAppPalette(
    name = "Manager",
    shadowAmbient = ManagerColors.BaseDeep,
    shadowSpot = ManagerColors.Turquoise,
    highlightTint = ManagerColors.TurquoiseLight,
    textPrimary = ManagerColors.TextPrimary,
    textSecondary = ManagerColors.TextSecondary,
    textAccent = ManagerColors.Gold,
)

val CustomerPalette = RojanAppPalette(
    name = "Customer",
    fillTint = HomeColors.Primary,
    shadowAmbient = HomeColors.Glow,
    shadowSpot = HomeColors.Glow,
    highlightTint = Color.White,
    textPrimary = HomeColors.TextPrimary,
    textSecondary = HomeColors.TextSecondary,
    textAccent = HomeColors.Glow,
)

/**
 * ROJAN_Reception_Implementation_Plan_v1.md, Phase 0 — the whole palette
 * layer's contribution for the Reception app: one new instance, zero new
 * glass/border/button/card/icon mechanic (see this file's own doc comment
 * and CLAUDE.md's "Shared Premium Glass Design System" section, which
 * names Reception explicitly as a pre-approved future app under this
 * exact rule). Reception rides on the shared, already-approved
 * [ai.rojan.designlab.ui.background.WarmBackground] (light) rather than a
 * bespoke dark theme like Manager's — there is no approved Reception
 * design reference the way Manager had one, so this deliberately reuses
 * existing tokens instead of inventing new raw hex. Amber (the existing
 * `RojanCategoryMakeup*` tokens) is unclaimed by both Manager
 * (Teal/Gold) and Customer (Purple/Pink/`HomeColors`), giving Reception
 * its own distinguishable identity without a new color system.
 */
val ReceptionPalette = RojanAppPalette(
    name = "Reception",
    fillTint = RojanCategoryMakeupStart,
    shadowAmbient = RojanCategoryMakeupIcon,
    shadowSpot = RojanCategoryMakeupIcon,
    highlightTint = Color.White,
    textPrimary = RojanTextPrimary,
    textSecondary = RojanTextSecondary,
    textAccent = RojanCategoryMakeupIcon,
)
