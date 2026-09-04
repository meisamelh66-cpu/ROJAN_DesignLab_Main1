package ai.rojan.designlab.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.screens.customer.hometheme.HomeColors

/**
 * Shared Premium Glass Design System — the one thing allowed to differ
 * between ROJAN apps (Manager, Customer, and future Specialist/Reception/
 * Accountant/Inventory apps) is this palette; every rendering mechanic that
 * consumes it ([ai.rojan.designlab.ui.components.glass.PremiumGlassSurface],
 * [ai.rojan.designlab.ui.components.buttons.PremiumButton]) is otherwise
 * identical across apps.
 *
 * Fields are limited to what the shared mechanics actually consume today —
 * no speculative fields for icon accents / card fills that later phases
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
    /**
     * Design-system refinement, Phase 3 — which [ai.rojan.designlab.ui.components.buttons.PremiumButton]
     * material this app's primary CTA renders as by default. Never
     * hardcoded at a call site; a screen that needs a different style
     * passes `PremiumButton`'s `style` parameter explicitly instead.
     */
    val buttonStyle: RojanButtonStyle,
    /**
     * Label color (and glass-wash accent stop) for a [RojanButtonStyle.Glass]
     * or [RojanButtonStyle.Outline] button. Deliberately separate from
     * [textAccent] — that field also drives bare-text roles (focus borders,
     * links) with their own stricter contrast requirements; a button's
     * label sits on its own bordered surface and can afford a different,
     * more saturated brand tone. Defaults to [textAccent] where the two
     * are already the same color (Manager, Customer).
     */
    val buttonAccent: Color = textAccent,
)

/** [RojanAppPalette.buttonStyle]'s three supported CTA materials — see [ai.rojan.designlab.ui.components.buttons.PremiumButton]. */
enum class RojanButtonStyle {
    /** Solid brand gradient fill — Customer's bold, high-contrast CTA. */
    Gradient,

    /** [ai.rojan.designlab.ui.components.glass.PremiumGlassSurface] + a low-alpha accent wash — Manager/Reception's restrained luxury CTA. */
    Glass,

    /** Transparent fill, accent-colored border and label only — no current app uses this by default; available for a screen that needs a lighter/secondary action in the same shape/typography language. */
    Outline,
}

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
    // Glass + Gold: the exact wash ManagerPrimaryButton already rendered
    // (Turquoise@0.20 -> Gold@0.16) — buttonAccent defaults to textAccent
    // (Gold), so this reproduces it unchanged now that PremiumButton is
    // the shared mechanic.
    buttonStyle = RojanButtonStyle.Glass,
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
    // Gradient: Customer's existing bold purple->magenta CTA, unchanged.
    buttonStyle = RojanButtonStyle.Gradient,
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
 * **Resolved (design-system refinement, Phase 5):** [textAccent] (focus
 * borders, links, spinners) previously stayed on the deferred makeup-amber
 * because no *pale/mid-tone* rose-gold in the token set clears 3:1 on
 * [ai.rojan.designlab.ui.background.WarmBackground] — precise WCAG
 * relative-luminance contrast was computed against `RojanWarmWhite`
 * (`#FFFBFF`) for every rose-gold-family candidate: [RojanPremiumBorderRoseGold]
 * ≈2.07:1, [RojanPremiumBorderGold] ≈2.05:1, [RojanRatingGold] ≈1.79:1 —
 * all fail. Only the family's darkest band, [RojanPremiumBorderShadow]
 * (already chosen for [buttonAccent] below), clears it: ≈**7.25:1** —
 * comfortably past not just the 3:1 UI-component bar but the stricter
 * 4.5:1 normal-text bar a "resend code" link needs. [textAccent] now uses
 * it too, so this field and [buttonAccent] are the same value again (as
 * they already are for Manager/Customer) — no new token invented, the
 * two-field split collapses back to one now that a safe shared value
 * exists.
 */
val ReceptionPalette = RojanAppPalette(
    name = "Reception",
    fillTint = RojanPremiumBorderRoseGold,
    shadowAmbient = RojanPremiumBorderRoseGold,
    shadowSpot = RojanPremiumBorderGold,
    highlightTint = Color.White,
    textPrimary = RojanTextPrimary,
    textSecondary = RojanTextSecondary,
    textAccent = RojanPremiumBorderShadow,
    buttonStyle = RojanButtonStyle.Glass,
    // buttonAccent defaults to textAccent — no longer needs an explicit
    // override now that textAccent itself is contrast-safe (see doc
    // comment above).
)
