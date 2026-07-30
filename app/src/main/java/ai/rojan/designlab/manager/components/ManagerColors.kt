package ai.rojan.designlab.manager.components

import androidx.compose.ui.graphics.Color

/**
 * ROJAN Manager dark luxury theme — Manager-only color tokens, entirely
 * separate from [ai.rojan.designlab.ui.theme.RojanTokens] (Customer's
 * shared palette, untouched). Values are exactly the approved reference
 * (`design/reference/ROJAN_Manager_Reference.png`) hex codes — nothing
 * invented beyond what was specified.
 *
 * Replaces the previous light-theme [ManagerAccent] (Teal/Gold aliased
 * onto Customer's `RojanCategorySkinIcon`/`RojanRatingGold`) — this is
 * now the single source of truth for Manager's color identity.
 */
object ManagerColors {

    // Dark luxury background base
    val BasePrimary = Color(0xFF063B3F)
    val BaseSecondary = Color(0xFF084C52)
    val BaseDeep = Color(0xFF041E2A)

    // Turquoise (glass edge light, active/primary accent)
    val Turquoise = Color(0xFF00C9C8)
    val TurquoiseLight = Color(0xFF38E8E1)

    // Gold (metallic accent, highlights)
    val Gold = Color(0xFFD4AF37)
    val GoldLight = Color(0xFFF5D77A)

    // Typography
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFC7D8D8)
    val TextAccent = Gold
}
