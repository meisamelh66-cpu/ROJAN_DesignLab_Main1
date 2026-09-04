package ai.rojan.designlab.ui.theme

import androidx.compose.ui.unit.dp

/**
 * ROJAN AI — corner-radius scale. **Single source of truth**: every
 * rounded corner in the design system comes from one of these four tiers.
 *
 * [RojanShapes] (the Compose [androidx.compose.ui.graphics.Shape]s the
 * whole app consumes) derives directly from these values — there is no
 * parallel radius ladder. The three previously-competing sources have
 * been reconciled onto this one:
 * - `RojanShapes` — now derives from here (was raw `.dp` literals).
 * - `RojanDimens.RadiusSM/MD/LG/XL` — removed (they had zero call sites
 *   and their values `12/20/32/48` did not match the shapes actually
 *   rendered).
 * - `Theme.kt`'s `RojanMaterialShapes` — the Material3 baseline; still
 *   independent, but nothing reads `MaterialTheme.shapes` today (see that
 *   file's own note). Folding it in is a follow-up.
 *
 * Values are exactly what the shipping Customer/Manager/Reception apps
 * already render (`Small 16` / `Card 32` / `Pill 50` / `Circle 100`) —
 * this consolidation names them, it changes no rendered corner.
 */
object RojanRadius {

    /** Chips, day-cells, toggles, badges, search fields, small glass surfaces. */
    val Small = 16.dp

    /** Standard glass card / panel — the app's default surface radius. */
    val Card = 32.dp

    /** Fully-rounded pill — the primary CTA shape. */
    val Pill = 50.dp

    /** Circular controls — icon buttons, avatars, the raised Home button. */
    val Circle = 100.dp
}
