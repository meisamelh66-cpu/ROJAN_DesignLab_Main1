package ai.rojan.designlab.ui.components.buttons

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.ui.theme.CustomerPalette
import ai.rojan.designlab.ui.theme.ManagerPalette
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanButtonStyle
import ai.rojan.designlab.ui.theme.RojanPremiumBorderShadow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Design-system refinement, Phase 3 — [PremiumButton] is now the single
 * primary-CTA component every app renders through; the only thing that
 * varies per app is [ai.rojan.designlab.ui.theme.RojanAppPalette.buttonStyle]
 * / `buttonAccent`. `PremiumButton` itself has no Compose UI test harness
 * available in this environment (no device/emulator — see this session's
 * standing environment notes), so these are the palette-level contract
 * tests: which style each app resolves to, and that the style/accent
 * choices are pinned (not a silent future drift).
 */
class PremiumButtonTest {

    // ---- style mapping -------------------------------------------

    @Test
    fun `Customer resolves to the Gradient CTA`() {
        assertEquals(RojanButtonStyle.Gradient, CustomerPalette.buttonStyle)
    }

    @Test
    fun `Manager resolves to the Glass CTA`() {
        assertEquals(RojanButtonStyle.Glass, ManagerPalette.buttonStyle)
    }

    @Test
    fun `Reception resolves to the Glass CTA - no longer inherits Customer's Gradient`() {
        // Before this pass Reception had no buttonStyle of its own and every
        // Reception screen's PremiumButton() call rendered Customer's
        // purple/magenta Gradient by accident. This pins the fix.
        assertEquals(RojanButtonStyle.Glass, ReceptionPalette.buttonStyle)
    }

    // ---- palette selection (buttonAccent) -------------------------

    @Test
    fun `Manager buttonAccent defaults to textAccent - Gold, unchanged from before unification`() {
        assertEquals(ManagerPalette.textAccent, ManagerPalette.buttonAccent)
        assertEquals(ManagerColors.Gold, ManagerPalette.buttonAccent)
    }

    @Test
    fun `Customer buttonAccent defaults to textAccent`() {
        assertEquals(CustomerPalette.textAccent, CustomerPalette.buttonAccent)
    }

    @Test
    fun `Reception buttonAccent is deliberately NOT textAccent - a contrast-safe rose-gold, not the deferred amber`() {
        assertNotEquals(ReceptionPalette.textAccent, ReceptionPalette.buttonAccent)
        assertEquals(RojanPremiumBorderShadow, ReceptionPalette.buttonAccent)
    }

    // ---- no duplicate / undecided behavior -------------------------

    @Test
    fun `every RojanButtonStyle is exactly the three documented CTA materials`() {
        assertEquals(
            "a new style must be an intentional addition, not a silent enum drift",
            listOf(RojanButtonStyle.Gradient, RojanButtonStyle.Glass, RojanButtonStyle.Outline),
            RojanButtonStyle.entries,
        )
    }

    @Test
    fun `each app palette resolves to a single distinct style choice - no two apps silently share a style struct by accident`() {
        // Manager and Reception both being Glass is intentional (asserted
        // above); this guards that Customer's Gradient stays the one
        // deliberately different app, not an accidental split.
        val styles = listOf(CustomerPalette, ManagerPalette, ReceptionPalette).map { it.buttonStyle }
        assertEquals(listOf(RojanButtonStyle.Gradient, RojanButtonStyle.Glass, RojanButtonStyle.Glass), styles)
    }
}
