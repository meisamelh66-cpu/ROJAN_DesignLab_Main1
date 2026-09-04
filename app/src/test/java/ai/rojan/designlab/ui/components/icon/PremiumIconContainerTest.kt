package ai.rojan.designlab.ui.components.icon

import ai.rojan.designlab.manager.components.ManagerIconTheme
import ai.rojan.designlab.ui.theme.RojanDimens
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Design-system refinement, Phase 4 — [PremiumIconContainer] has no
 * Compose UI test harness available in this environment (no device —
 * same standing limitation as [ai.rojan.designlab.ui.components.buttons.PremiumButtonTest]),
 * so these are the contract-level tests: the token defaults it exposes,
 * and that [ai.rojan.designlab.manager.components.ManagerIconContainer]'s
 * wrapper hasn't drifted from them.
 */
class PremiumIconContainerTest {

    @Test
    fun `RojanIconContainerStyle is exactly the two documented container materials`() {
        assertEquals(
            "a new style must be an intentional addition, not a silent enum drift",
            listOf(RojanIconContainerStyle.Glass, RojanIconContainerStyle.Gradient),
            RojanIconContainerStyle.entries,
        )
    }

    @Test
    fun `default container size reuses the existing 48dp min-touch-target token, not a new dimension`() {
        assertEquals(RojanDimens.MinTouchTarget, PremiumIconContainerDefaults.ContainerSize)
    }

    @Test
    fun `default glow multiplier is unchanged from before unification`() {
        assertEquals(1.6f, PremiumIconContainerDefaults.GlowSizeMultiplier, 0f)
    }

    @Test
    fun `ManagerIconTheme's defaults have not drifted from PremiumIconContainerDefaults`() {
        assertEquals(PremiumIconContainerDefaults.ContainerSize, ManagerIconTheme.ContainerSize)
        assertEquals(PremiumIconContainerDefaults.GlowSizeMultiplier, ManagerIconTheme.GlowSizeMultiplier, 0f)
    }
}
