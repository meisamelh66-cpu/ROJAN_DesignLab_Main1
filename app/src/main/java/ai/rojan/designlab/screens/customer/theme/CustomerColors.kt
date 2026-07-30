package ai.rojan.designlab.screens.customer.theme

import ai.rojan.designlab.ui.theme.RojanAquaMint
import ai.rojan.designlab.ui.theme.RojanBlushPink
import ai.rojan.designlab.ui.theme.RojanRose
import ai.rojan.designlab.ui.theme.RojanSoftLavender
import ai.rojan.designlab.ui.theme.RojanWarmWhite
import androidx.compose.ui.graphics.Color

/**
 * ROJAN Customer feminine luxury theme. Currently wired into
 * [ai.rojan.designlab.screens.splash.SplashScreen]'s background only
 * (Phase 1) — Home/Search/Profile/`CustomerBottomBar` are untouched,
 * pending approval per the phased implementation strategy. Aliases the
 * existing, already-frozen [ai.rojan.designlab.ui.theme.RojanTokens]
 * values directly (no duplicated hex for anything that already has a
 * token) plus the genuinely new Rose Gold accents and Pearl Pink.
 */
object CustomerColors {
    /** Warm white base — existing token, unchanged. */
    val WarmWhite = RojanWarmWhite

    /** Secondary atmosphere — genuinely new, not previously in RojanTokens.kt. */
    val PearlPink = Color(0xFFFFF4FA)

    /** Pink accents — existing tokens, unchanged. */
    val Rose = RojanRose
    val BlushPink = RojanBlushPink

    /** Lavender — existing token, unchanged. */
    val SoftLavender = RojanSoftLavender

    /** Aqua highlight — existing token, unchanged. */
    val AquaMint = RojanAquaMint

    /** Rose Gold — genuinely new accent, not previously in RojanTokens.kt. */
    val RoseGold = Color(0xFFD9A066)
    val SoftRoseGold = Color(0xFFE8BFA8)

    // Typography
    val TextPrimary = Color(0xFF2B1B3D)
    val TextSecondary = Color(0xFF75647F)
    val TextAccent = RoseGold
}
