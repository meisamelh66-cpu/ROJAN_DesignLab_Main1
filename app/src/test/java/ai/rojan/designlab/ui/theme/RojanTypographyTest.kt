package ai.rojan.designlab.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Phase 2B — the ROJAN typography system now renders through **Vazirmatn**
 * with Persian line metrics. This pins:
 *  - the family is a real bundled face, not the platform fallback;
 *  - every named style routes through the one [RojanFontFamily] swap point;
 *  - font sizes are unchanged (the tuning pass moved line-height only);
 *  - the Persian-tuned line-heights;
 *  - zero letter-spacing everywhere (positive tracking breaks Persian's
 *    connected script) — including the Material `bodyLarge` floor, which
 *    previously carried the Latin `0.5.sp`;
 *  - the shared centred / no-trim line-box behaviour.
 */
class RojanTypographyTest {

    private val allStyles: Map<String, TextStyle> = mapOf(
        "Display" to RojanTypography.Display,
        "ScreenTitle" to RojanTypography.ScreenTitle,
        "SectionTitle" to RojanTypography.SectionTitle,
        "CardTitle" to RojanTypography.CardTitle,
        "HeroTitle" to RojanTypography.HeroTitle,
        "Body" to RojanTypography.Body,
        "Button" to RojanTypography.Button,
        "Caption" to RojanTypography.Caption,
    )

    // ---- family --------------------------------------------------

    @Test
    fun `RojanFontFamily is a real bundled face, not the platform fallback`() {
        assertNotEquals(FontFamily.Default, RojanFontFamily)
    }

    @Test
    fun `every named style routes through the single RojanFontFamily swap point`() {
        allStyles.forEach { (name, style) ->
            assertSame("$name.fontFamily", RojanFontFamily, style.fontFamily)
        }
        assertSame("bodyLarge.fontFamily", RojanFontFamily, RojanBaseTypography.bodyLarge.fontFamily)
    }

    // ---- sizes unchanged (the "keep existing font sizes" constraint) ----

    @Test
    fun `font sizes are unchanged by the Persian line-metric pass`() {
        assertEquals(34f, RojanTypography.Display.fontSize.value, 0f)
        assertEquals(30f, RojanTypography.ScreenTitle.fontSize.value, 0f)
        assertEquals(24f, RojanTypography.SectionTitle.fontSize.value, 0f)
        assertEquals(20f, RojanTypography.CardTitle.fontSize.value, 0f)
        assertEquals(32f, RojanTypography.HeroTitle.fontSize.value, 0f)
        assertEquals(17f, RojanTypography.Body.fontSize.value, 0f)
        assertEquals(16f, RojanTypography.Button.fontSize.value, 0f)
        assertEquals(15f, RojanTypography.Caption.fontSize.value, 0f)
    }

    // ---- Persian-tuned line-heights ----------------------------

    @Test
    fun `line-heights match the Persian tuning table`() {
        assertEquals(46f, RojanTypography.Display.lineHeight.value, 0f)
        assertEquals(42f, RojanTypography.ScreenTitle.lineHeight.value, 0f)
        assertEquals(34f, RojanTypography.SectionTitle.lineHeight.value, 0f)
        assertEquals(30f, RojanTypography.CardTitle.lineHeight.value, 0f)
        assertEquals(42f, RojanTypography.HeroTitle.lineHeight.value, 0f)
        assertEquals(28f, RojanTypography.Body.lineHeight.value, 0f)
        assertEquals(20f, RojanTypography.Button.lineHeight.value, 0f)
        assertEquals(24f, RojanTypography.Caption.lineHeight.value, 0f)
        assertEquals(26f, RojanBaseTypography.bodyLarge.lineHeight.value, 0f)
    }

    // ---- letter-spacing ---------------------------------------

    @Test
    fun `letter-spacing is zero on every style, including the Material bodyLarge floor`() {
        allStyles.forEach { (name, style) ->
            assertEquals("$name.letterSpacing", 0f, style.letterSpacing.value, 0f)
        }
        assertEquals(
            "bodyLarge.letterSpacing (was 0.5.sp)",
            0f,
            RojanBaseTypography.bodyLarge.letterSpacing.value,
            0f,
        )
    }

    // ---- line-box behaviour -----------------------------------

    @Test
    fun `every style carries the shared centred, no-trim line box`() {
        allStyles.forEach { (name, style) ->
            assertNotNull("$name.platformStyle", style.platformStyle)
            val lhs = style.lineHeightStyle
            assertNotNull("$name.lineHeightStyle", lhs)
            assertEquals("$name line-box alignment", LineHeightStyle.Alignment.Center, lhs!!.alignment)
            assertEquals("$name line-box trim", LineHeightStyle.Trim.None, lhs.trim)
        }
    }
}
