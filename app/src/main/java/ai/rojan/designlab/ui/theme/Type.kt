package ai.rojan.designlab.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

/**
 * ROJAN AI named typography tokens (Design Token Specification v1.0,
 * Section 3). Only the two levels the current Component Migration phase
 * needs are defined so far (HeroTitle, Body) — the remaining 5 levels
 * (Display, ScreenTitle, SectionTitle, Caption, Button) get added as the
 * components that need them are migrated, not all at once up front.
 *
 * Font family: [FontFamily.Default] here is a placeholder. The approved
 * spec's target is Vazirmatn, but integrating real font files is asset
 * work explicitly out of scope for this phase ("Do not create final
 * assets") — swapping this to Vazirmatn later is a one-line change per
 * style once the font files exist, nothing that touches call sites.
 */
object RojanTypography {

    /**
     * UI Consolidation Sprint v2.0: complete 6-level hierarchy, exact
     * sizes as specified ("Suggested sizes... Never use unreadable tiny
     * text"). [HeroTitle]/[Button] predate this pass and are left at
     * their existing sizes (32sp/16sp) rather than force-fit into this
     * new scale — both were already reasonably sized and are used in
     * fixed-height layouts ([ai.rojan.designlab.components.hero.HeroBookingCard]'s
     * 360dp budget, [ai.rojan.designlab.ui.components.buttons.PremiumButton]'s
     * fixed pill) where an uncoordinated size bump risks real overflow,
     * not just a visual change - a genuinely different risk profile than
     * [Body]/[Caption], which are used in flexible, wrap-content contexts
     * throughout the app.
     */

    /** Display Title. 34sp/Bold. */
    val Display = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
    )

    /** Screen Title. 30sp/Bold. */
    val ScreenTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
    )

    /** Section Title. 24sp/SemiBold. */
    val SectionTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    )

    /** Card Title. 20sp/SemiBold - distinct from the smaller, more general [Body]. */
    val CardTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    )

    /** Hero Card titles. 32sp/Bold — unchanged from before this pass, see class doc comment. */
    val HeroTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    )

    /**
     * Default body text — also what most card titles/names use
     * throughout this codebase.
     *
     * UI Consolidation Sprint v2.0: 15sp -> 17sp ("Body → 17sp... the
     * current typography is too small... Persian text must be
     * comfortably readable on 6-7 inch phones") — the single highest-
     * impact size change in this pass, since [Body] is this codebase's
     * most-used text style.
     */
    val Body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    )

    /**
     * Button labels. 16sp/SemiBold — unchanged from before this pass,
     * see class doc comment (fixed-height button budget risk).
     */
    val Button = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    )

    /**
     * Caption / secondary-meta text.
     *
     * UI Consolidation Sprint v2.0: 12sp -> 15sp ("Caption → 15sp...
     * Never use unreadable tiny text") - this was the smallest text
     * size in the app and the most likely to be genuinely hard to read.
     */
    val Caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    )
}