package ai.rojan.designlab.ui.components.cards

import ai.rojan.designlab.ui.components.glass.PremiumGlassTheme
import ai.rojan.designlab.ui.theme.RojanShadows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Design-system refinement, Phase 4 — foundation-only pass:
 * [PremiumCardShell] migrates no screen yet (see the Phase 4 report's
 * migration plan), so there is no rendered call site to assert against.
 * [rojanCardVariantSpec] was deliberately kept a plain function (no
 * [androidx.compose.runtime.Composable]) specifically so the one thing
 * that actually varies per [RojanCardVariant] is unit-testable without a
 * Compose UI test harness (unavailable in this environment — same
 * standing limitation as [ai.rojan.designlab.ui.components.buttons.PremiumButtonTest]
 * / [ai.rojan.designlab.ui.components.icon.PremiumIconContainerTest]).
 */
class PremiumCardShellTest {

    @Test
    fun `RojanCardVariant is exactly the three documented card materials`() {
        assertEquals(
            "a new variant must be an intentional addition, not a silent enum drift",
            listOf(RojanCardVariant.GlassCard, RojanCardVariant.SolidCard, RojanCardVariant.HighlightCard),
            RojanCardVariant.entries,
        )
    }

    @Test
    fun `GlassCard uses the standard PremiumGlassSurface defaults and Floating elevation`() {
        val spec = rojanCardVariantSpec(RojanCardVariant.GlassCard)
        assertEquals(PremiumGlassTheme.FillAlpha, spec.fillAlpha, 0f)
        assertEquals(PremiumGlassTheme.FillSecondaryAlpha, spec.fillSecondaryAlpha, 0f)
        assertEquals(PremiumGlassTheme.BorderAlpha, spec.borderAlpha, 0f)
        assertEquals(RojanShadows.FloatingElevation, spec.elevation)
    }

    @Test
    fun `SolidCard is near-opaque and uses the resting Soft elevation`() {
        val spec = rojanCardVariantSpec(RojanCardVariant.SolidCard)
        assertEquals("solid must read more opaque than glass", 0.85f, spec.fillAlpha, 0f)
        assertEquals(RojanShadows.SoftElevation, spec.elevation)
        // still strictly higher fill than the standard glass card - never accidentally the more translucent one
        assertTrue(spec.fillAlpha > rojanCardVariantSpec(RojanCardVariant.GlassCard).fillAlpha)
    }

    @Test
    fun `HighlightCard is more filled than GlassCard and uses the hero-level Premium elevation`() {
        val spec = rojanCardVariantSpec(RojanCardVariant.HighlightCard)
        val glass = rojanCardVariantSpec(RojanCardVariant.GlassCard)
        assertEquals(RojanShadows.PremiumElevation, spec.elevation)
        assertTrue(spec.fillAlpha > glass.fillAlpha)
        assertTrue(spec.fillSecondaryAlpha > glass.fillSecondaryAlpha)
        // border reads exactly as prominent as the standard card, not dimmer
        assertEquals(PremiumGlassTheme.BorderAlpha, spec.borderAlpha, 0f)
    }

    @Test
    fun `elevation strictly increases Solid less-than Glass less-than Highlight, matching the app's shadow-by-intent rule`() {
        val solid = rojanCardVariantSpec(RojanCardVariant.SolidCard).elevation
        val glass = rojanCardVariantSpec(RojanCardVariant.GlassCard).elevation
        val highlight = rojanCardVariantSpec(RojanCardVariant.HighlightCard).elevation
        assertTrue(solid < glass)
        assertTrue(glass < highlight)
    }
}
