package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.theme.RojanCategorySkinIcon
import ai.rojan.designlab.ui.theme.RojanRatingGold

/**
 * Manager App workspace accent identity — Teal + Gold, distinct from
 * Customer App's purple/pink identity. Both values are existing
 * [ai.rojan.designlab.ui.theme] colors (no new hex introduced anywhere
 * in the Manager module); this just names which two of them are "the
 * Manager identity" so every Manager component references one shared
 * pair instead of each picking independently.
 */
object ManagerAccent {
    val Teal = RojanCategorySkinIcon
    val Gold = RojanRatingGold
}

/**
 * Manager App workspace glass tuning — more opaque than the shared
 * [ai.rojan.designlab.ui.components.glass.GlassSurface] default (0.46f/
 * 0.18f fill, 0.24f/0.12f border), per the "semi-opaque glass, strong
 * text contrast, avoid over-transparent glass" readability direction.
 * Passed as explicit parameters to the existing, unmodified
 * `GlassSurface` at each Manager call site — not a fork of the shared
 * component, and Customer screens keep the original defaults untouched.
 */
object ManagerGlass {
    const val Alpha = 0.72f
    const val SecondaryAlpha = 0.5f
    const val BorderAlpha = 0.35f
    const val BorderSecondaryAlpha = 0.2f
}
