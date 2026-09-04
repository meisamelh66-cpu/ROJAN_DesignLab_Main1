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
 * Still deferred: [textAccent] (focus borders, links, spinners) is left
 * on the existing value — a rose-gold that also clears the 3:1 UI-contrast
 * bar on soft white is not among the current tokens, so choosing that
 * accent needs a designer + a contrast check on-device, not a blind pick.
 *
 * **Phase 3 — button system:** [buttonStyle] = [RojanButtonStyle.Glass]
 * moves Reception's primary CTA off Customer's purple/magenta
 * [RojanButtonStyle.Gradient] (what every Reception screen rendered before
 * this pass — `PremiumButton` had no per-app style, so Reception simply
 * got Customer's) onto its own rose-gold glass identity. [buttonAccent]
 * is deliberately **not** [textAccent] (still the deferred amber): it uses
 * [RojanPremiumBorderShadow], the metallic border's darkened-bronze band —
 * same rose-gold/gold family as the glass edge, and ≈5.9:1 contrast on
 * [ai.rojan.designlab.ui.background.WarmBackground], comfortably clearing
 * the bar a primary CTA's label needs.
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
    buttonStyle = RojanButtonStyle.Glass,
    buttonAccent = RojanPremiumBorderShadow,
)
