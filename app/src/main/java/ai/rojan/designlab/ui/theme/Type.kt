package ai.rojan.designlab.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * ROJAN typeface — the **single swap point** for the app's font family.
 *
 * Today this is [FontFamily.Default] (the platform default, which shapes
 * Persian through whatever fallback face the device ships). The approved
 * design-spec target is **Vazirmatn**. Font-asset integration is a
 * follow-up step (the Vazirmatn font files are not yet in the repo): once
 * they are added under `app/src/main/res/font/` and a `FontFamily` is
 * built from them, **this one declaration is the only line that changes** —
 * every [RojanTypography] style and the Material [RojanBaseTypography]
 * below already route through it, so no screen or component is touched.
 *
 * New code must reference this, never [FontFamily.Default] directly.
 */
val RojanFontFamily: FontFamily = FontFamily.Default

/**
 * ROJAN font-weight scale — named roles instead of bare [FontWeight]
 * literals spread across styles and components.
 *
 * The four weights [RojanTypography] actually uses ([Bold] 700 /
 * [SemiBold] 600 / [Medium] 500 / [Regular] 400) are unchanged in value
 * from before this mapping existed — this only gives them names. [Light]
 * (300) and [ExtraBold] (800) are declared for the display hierarchy
 * Vazirmatn's weight range will support once integrated; they are not
 * applied anywhere yet.
 *
 * Role guidance:
 * - [Bold]      — Display / Screen / Section / Hero titles
 * - [SemiBold]  — Card titles, greetings, button labels
 * - [Medium]    — body text, emphasised captions
 * - [Regular]   — captions, secondary / meta text
 * - [Light]     — reserved (large display numerals, post-Vazirmatn)
 * - [ExtraBold] — reserved (hero display numerals, post-Vazirmatn)
 */
object RojanFontWeights {
    val Light: FontWeight = FontWeight.Light        // 300
    val Regular: FontWeight = FontWeight.Normal     // 400
    val Medium: FontWeight = FontWeight.Medium      // 500
    val SemiBold: FontWeight = FontWeight.SemiBold  // 600
    val Bold: FontWeight = FontWeight.Bold          // 700
    val ExtraBold: FontWeight = FontWeight.ExtraBold // 800
}

/**
 * Material3 base text style — a floor for anything that reads
 * `MaterialTheme.typography.bodyLarge` directly. Routed through
 * [RojanFontFamily] / [RojanFontWeights] so a future Vazirmatn swap
 * covers it too. The real hierarchy is [RojanTypography] (below) plus
 * `RojanMaterialTypography` in `Theme.kt`, which is what `RojanTheme`
 * actually installs.
 */
val RojanBaseTypography = androidx.compose.material3.Typography(
    bodyLarge = TextStyle(
        fontFamily = RojanFontFamily,
        fontWeight = RojanFontWeights.Regular,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * ROJAN AI named typography tokens (Design Token Specification v1.0,
 * Section 3) — the app's single text hierarchy.
 *
 * UI Consolidation Sprint v2.0: complete 6-level hierarchy, exact sizes
 * as specified ("Suggested sizes... Never use unreadable tiny text").
 * [HeroTitle]/[Button] predate that pass and are left at their existing
 * sizes (32sp/16sp) rather than force-fit into the new scale — both were
 * already reasonably sized and are used in fixed-height layouts
 * ([ai.rojan.designlab.components.hero.HeroBookingCard]'s 360dp budget,
 * [ai.rojan.designlab.ui.components.buttons.PremiumButton]'s fixed pill)
 * where an uncoordinated size bump risks real overflow.
 *
 * Font family + weights route through [RojanFontFamily] / [RojanFontWeights]
 * — see those declarations. Sizes and line-heights are unchanged by the
 * UI Polish Sprint 1 typography-foundation pass (that pass only introduced
 * the family/weight swap points; a Persian line-height re-tune is a
 * separate, later step gated on the Vazirmatn asset).
 */
object RojanTypography {

    /** Display Title. 34sp/Bold. */
    val Display = TextStyle(
        fontFamily = RojanFontFamily,
        fontWeight = RojanFontWeights.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
    )

    /** Screen Title. 30sp/Bold. */
    val ScreenTitle = TextStyle(
        fontFamily = RojanFontFamily,
        fontWeight = RojanFontWeights.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
    )

    /** Section Title. 24sp/SemiBold. */
    val SectionTitle = TextStyle(
        fontFamily = RojanFontFamily,
        fontWeight = RojanFontWeights.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    )

    /** Card Title. 20sp/SemiBold - distinct from the smaller, more general [Body]. */
    val CardTitle = TextStyle(
        fontFamily = RojanFontFamily,
        fontWeight = RojanFontWeights.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    )

    /** Hero Card titles. 32sp/Bold — unchanged from before the v2.0 pass, see class doc comment. */
    val HeroTitle = TextStyle(
        fontFamily = RojanFontFamily,
        fontWeight = RojanFontWeights.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    )

    /**
     * Default body text — also what most card titles/names use throughout
     * this codebase.
     *
     * UI Consolidation Sprint v2.0: 15sp -> 17sp ("Body → 17sp... the
     * current typography is too small... Persian text must be comfortably
     * readable on 6-7 inch phones") — the single highest-impact size
     * change in that pass, since [Body] is this codebase's most-used text
     * style.
     */
    val Body = TextStyle(
        fontFamily = RojanFontFamily,
        fontWeight = RojanFontWeights.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    )

    /**
     * Button labels. 16sp/SemiBold — unchanged from before the v2.0 pass,
     * see class doc comment (fixed-height button budget risk).
     */
    val Button = TextStyle(
        fontFamily = RojanFontFamily,
        fontWeight = RojanFontWeights.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    )

    /**
     * Caption / secondary-meta text.
     *
     * UI Consolidation Sprint v2.0: 12sp -> 15sp ("Caption → 15sp... Never
     * use unreadable tiny text") - this was the smallest text size in the
     * app and the most likely to be genuinely hard to read.
     */
    val Caption = TextStyle(
        fontFamily = RojanFontFamily,
        fontWeight = RojanFontWeights.Regular,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    )
}
