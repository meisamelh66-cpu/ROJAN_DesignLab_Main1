package ai.rojan.designlab.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Design-system refinement Phase 1 — [RojanRadius] is the single
 * corner-radius source of truth. This pins the four tiers and verifies
 * [RojanShapes] derives every shape from them, so a stray `.dp` literal
 * creeping back into `Shapes.kt` (the exact drift this consolidation
 * removed) fails the build.
 */
class RojanRadiusTest {

    @Test
    fun `radius ladder is the four canonical tiers`() {
        assertEquals(16f, RojanRadius.Small.value, 0f)
        assertEquals(32f, RojanRadius.Card.value, 0f)
        assertEquals(50f, RojanRadius.Pill.value, 0f)
        assertEquals(100f, RojanRadius.Circle.value, 0f)
    }

    @Test
    fun `every RojanShapes shape derives from a RojanRadius tier`() {
        assertEquals(RoundedCornerShape(RojanRadius.Small), RojanShapes.Small)
        assertEquals(RoundedCornerShape(RojanRadius.Card), RojanShapes.GlassCard)
        assertEquals(RoundedCornerShape(RojanRadius.Pill), RojanShapes.PremiumButton)
        assertEquals(RoundedCornerShape(RojanRadius.Circle), RojanShapes.Circle)
    }

    @Test
    fun `GlassCard stays a uniform all-corner 32dp radius after the literal-to-token swap`() {
        // previously: RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 32.dp, bottomEnd = 32.dp)
        assertEquals(RoundedCornerShape(32.dp), RojanShapes.GlassCard)
    }
}
