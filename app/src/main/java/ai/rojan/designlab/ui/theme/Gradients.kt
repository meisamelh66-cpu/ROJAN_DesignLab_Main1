package ai.rojan.designlab.ui.theme

import androidx.compose.ui.graphics.Brush

/**
 * Theme & Typography Architecture Normalization: this object previously
 * held 6 gradients, 5 of which were confirmed dead (zero real code
 * usage anywhere in the project — [DeepBackground], [NeonRing]... wait,
 * see removal note below) with their own separately-hardcoded hex
 * values duplicating colors [RojanTokens] already defines. Removed to
 * restore a single source of truth for color; only [PremiumButton]
 * (the one genuinely used member, confirmed via direct search — used by
 * [ai.rojan.designlab.ui.components.buttons.PremiumButton]) remains,
 * now built from [RojanVividPurple]/[RojanVividMagenta] directly instead
 * of its own separate near-duplicate hex literals
 * (`0xFF8E2DE2`/`0xFFFF4FD8`, which were already almost identical to
 * those two tokens).
 *
 * Removed members and why each was safe to remove (all confirmed zero
 * usage via project-wide search before deletion):
 * - `Background`: only ever mentioned in a doc comment describing
 *   history, never actually called as code.
 * - `DeepBackground`, `NeonRing`, `Glass`, `Highlight`: zero references
 *   anywhere, including comments.
 */
object RojanGradients {

    /** Premium button fill — brand purple → magenta, built from the approved tokens directly. */
    val PremiumButton = Brush.linearGradient(
        colors = listOf(RojanVividPurple, RojanVividMagenta)
    )
}
