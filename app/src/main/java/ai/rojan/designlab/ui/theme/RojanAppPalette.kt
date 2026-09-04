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
 * glass/border/button/card/icon mechanic. Reception rides on the shared,
 * already-approved [ai.rojan.designlab.ui.background.WarmBackground]
 * (soft white) — the "light luxury operational" canvas — rather than a
 * bespoke dark theme like Manager's.
 *
 * Premium direction (design-system refinement, Phase 1): the glass
 * surfaces and their shadows now carry the shared **rose-gold / gold
 * metallic** language ([RojanPremiumBorderRoseGold] / [RojanPremiumBorderGold]
 * — the same tokens every glass edge already uses) instead of the earlier
 * makeup-amber. This removes the "enterprise orange" read from every
 * Reception panel while staying inside the existing token vocabulary
 * (no new hex).
 *
 * Still deferred: [textAccent] (focus borders, links, spinners) is left
 * on the existing value — a rose-gold that also clears the 3:1 UI-contrast
 * bar on soft white is not among the current tokens, so choosing that
 * accent needs a designer + a contrast check on-device, not a blind pick.
 */
val ReceptionPalette = RojanAppPalette(
    name = "Reception",
    fillTint = RojanPremiumBorderRoseGold,
    shadowAmbient = RojanPremiumBorderRoseGold,
    shadowSpot = RojanPremiumBorderGold,
    highlightTint = Color.White,
    textPrimary = RojanTextPrimary,
    textSecondary = RojanTextSecondary,
    textAccent = RojanCategoryMakeupIcon,
)
