package ai.rojan.designlab.ui.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

import ai.rojan.designlab.R

/**
 * ROJAN typeface — **Vazirmatn** (SIL OFL 1.1), bundled as four static
 * weights under `res/font/` (`vazirmatn_regular/medium/semibold/bold.ttf`;
 * licence at `assets/fonts/OFL.txt`). A modern Persian-first geometric
 * sans — the design-spec target, replacing the platform `FontFamily.Default`
 * fallback the app shipped through Phase 1.
 *
 * This is still the **single swap point** for the family: every
 * [RojanTypography] style and the Material [RojanBaseTypography] below
 * route through it, so no screen or component references a font family
 * directly. New code must reference this, never [FontFamily] literals.
 *
 * Static weights (not the variable font) deliberately: `minSdk = 24`, and
 * `Font(variationSettings = …)` needs API 26+ — static files render the
 * correct weight on every supported device.
 */
val RojanFontFamily: FontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, RojanFontWeights.Regular),
    Font(R.font.vazirmatn_medium, RojanFontWeights.Medium),
    Font(R.font.vazirmatn_semibold, RojanFontWeights.SemiBold),
    Font(R.font.vazirmatn_bold, RojanFontWeights.Bold),
)

/**
 * ROJAN font-weight scale — named roles instead of bare [FontWeight]
 * literals spread across styles and components.
 *
 * The four weights [RojanTypography] uses ([Bold] 700 / [SemiBold] 600 /
 * [Medium] 500 / [Regular] 400) are each backed by a real Vazirmatn file.
 * [Light] (300) and [ExtraBold] (800) are declared for a future display
 * hierarchy; Vazirmatn ships those weights, but the matching `.ttf` files
 * are not bundled yet, so they currently resolve to the nearest loaded
 * weight — do not apply them until their files are added.
 *
 * Role guidance:
 * - [Bold]      — Display / Screen / Section / Hero titles
 * - [SemiBold]  — Card titles, greetings, button labels
 * - [Medium]    — body text, emphasised captions
 * - [Regular]   — captions, secondary / meta text
 * - [Light]     — reserved (large display numerals)
 * - [ExtraBold] — reserved (hero display numerals)
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
 * Shared line-box behaviour for every ROJAN text style.
 *
 * Persian text carries diacritics above the baseline (zeer/zebar/tashdid)
 * and deep descenders; the platform's default line-box distribution biases
 * the extra `lineHeight` space to the top, which reads unevenly and can
 * clip a diacritic against the line above. [LineHeightStyle.Alignment.Center]
 * distributes the slack evenly; [LineHeightStyle.Trim.None] keeps the full
 * line box on the first and last line so nothing is shaved.
 *
 * [PlatformTextStyle] `includeFontPadding = false` removes the legacy
 * Android font-padding so the measured text box matches the real glyph
 * metrics — required for the fixed-height rows (calendar day-cells, list
 * rows) to centre Persian text correctly.
 */
@Suppress("DEPRECATION")
private val RojanPlatformTextStyle = PlatformTextStyle(includeFontPadding = false)

private val RojanLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/**
 * One builder for every [RojanTypography] style — guarantees they all
 * carry [RojanFontFamily], zero letter-spacing (positive tracking breaks
 * Persian's connected script), and the shared line-box behaviour.
 */
private fun rojanTextStyle(
    weight: FontWeight,
    fontSize: TextUnit,
    lineHeight: TextUnit,
): TextStyle = TextStyle(
    fontFamily = RojanFontFamily,
    fontWeight = weight,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = 0.sp,
    platformStyle = RojanPlatformTextStyle,
    lineHeightStyle = RojanLineHeightStyle,
)

/**
 * Material3 base text style — a floor for anything that reads
 * `MaterialTheme.typography.bodyLarge` directly. Routed through
 * [RojanFontFamily] and given the same Persian line metrics as
 * [RojanTypography.Body]. `letterSpacing` is `0.sp` (was `0.5.sp` — a
 * Latin default that degrades Persian letter-joining). The real hierarchy
 * is [RojanTypography] plus `RojanMaterialTypography` in `Theme.kt`, which
 * is what `RojanTheme` installs.
 */
val RojanBaseTypography = androidx.compose.material3.Typography(
    bodyLarge = rojanTextStyle(
        weight = RojanFontWeights.Regular,
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
)

/**
 * ROJAN AI named typography tokens — the app's single text hierarchy.
 *
 * **Persian line-metric pass (Phase 2B):** font sizes are unchanged; only
 * `lineHeight` moved — the previous values were Latin-tuned (Body ≈ 1.41×)
 * and too tight for Vazirmatn's Persian ascenders/descenders. New ratios
 * are ≈ 1.65× for body/caption and ≈ 1.35–1.45× for titles. [HeroTitle]
 * is kept deliberately tighter (1.31×) — it lives inside
 * [ai.rojan.designlab.components.hero.HeroBookingCard]'s fixed 360 dp
 * budget. [Button] line-height is unchanged (single line in a fixed pill).
 * Every style also now carries `letterSpacing = 0`, `includeFontPadding =
 * false`, and centred line-box trim, via [rojanTextStyle].
 *
 * Sizes: verified on device is still pending — a Persian line-height is a
 * visual judgement and these are the analysis-pass starting values.
 */
object RojanTypography {

    /** Display Title. 34sp/Bold. */
    val Display = rojanTextStyle(RojanFontWeights.Bold, 34.sp, 46.sp)

    /** Screen Title. 30sp/Bold. */
    val ScreenTitle = rojanTextStyle(RojanFontWeights.Bold, 30.sp, 42.sp)

    /** Section Title. 24sp/SemiBold. */
    val SectionTitle = rojanTextStyle(RojanFontWeights.SemiBold, 24.sp, 34.sp)

    /** Card Title. 20sp/SemiBold — distinct from the smaller, more general [Body]. */
    val CardTitle = rojanTextStyle(RojanFontWeights.SemiBold, 20.sp, 30.sp)

    /** Hero Card titles. 32sp/Bold — line-height kept tight for the fixed Hero budget. */
    val HeroTitle = rojanTextStyle(RojanFontWeights.Bold, 32.sp, 42.sp)

    /**
     * Default body text — also what most card titles/names use throughout
     * this codebase. 17sp/Medium, line-height ≈ 1.65× for comfortable
     * Persian reading on 6–7 inch phones.
     */
    val Body = rojanTextStyle(RojanFontWeights.Medium, 17.sp, 28.sp)

    /** Button labels. 16sp/SemiBold — single line, fixed-height pill: line-height unchanged. */
    val Button = rojanTextStyle(RojanFontWeights.SemiBold, 16.sp, 20.sp)

    /** Caption / secondary-meta text. 15sp/Regular. */
    val Caption = rojanTextStyle(RojanFontWeights.Regular, 15.sp, 24.sp)
}
