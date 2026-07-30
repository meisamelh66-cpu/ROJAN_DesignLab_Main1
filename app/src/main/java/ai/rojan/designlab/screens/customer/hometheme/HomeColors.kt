package ai.rojan.designlab.screens.customer.hometheme

import androidx.compose.ui.graphics.Color

import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanBlushPink
import ai.rojan.designlab.ui.theme.RojanDeepPurple
import ai.rojan.designlab.ui.theme.RojanNavy
import ai.rojan.designlab.ui.theme.RojanRatingGold
import ai.rojan.designlab.ui.theme.RojanSoftLavender
import ai.rojan.designlab.ui.theme.RojanVividMagenta
import ai.rojan.designlab.ui.theme.RojanVividPurple
import ai.rojan.designlab.ui.theme.RojanWarmWhite

/**
 * Customer Home dark-luxury theme — Home Visual Language Unification.
 *
 * Scoped to `CustomerHomeScreen` and the sections it composes only, per
 * the approved reference (`design/reference/ROJAN_Manager_Reference (2).png`).
 * Every value here aliases an existing [ai.rojan.designlab.ui.theme]
 * token rather than inventing new hex — [RojanNavy]/[RojanDeepPurple]
 * (already-defined dark-canvas tokens), [RojanAIGlow]/[RojanVividPurple]/
 * [RojanSoftLavender]/[RojanBlushPink]/[RojanVividMagenta]/[RojanRatingGold]
 * (existing brand accents) — except [TextSecondary]/[TextMuted], which
 * are genuinely new: the existing [ai.rojan.designlab.ui.theme.RojanTextSecondary]/
 * [ai.rojan.designlab.ui.theme.RojanMutedText] family is tuned for text
 * on light/warm-white surfaces and would be unreadable on this dark
 * canvas.
 */
object HomeColors {
    // Background
    val NavyBase = RojanNavy
    val DeepPurple = RojanDeepPurple

    // Accents
    val Glow = RojanAIGlow
    val Primary = RojanVividPurple
    val Lavender = RojanSoftLavender
    val Rose = RojanBlushPink
    val Magenta = RojanVividMagenta
    val Gold = RojanRatingGold

    // Text on dark
    val TextPrimary = RojanWarmWhite
    val TextSecondary = Color(0xFFCBBEE0)
    val TextMuted = Color(0xFF9C8FB5)
}
