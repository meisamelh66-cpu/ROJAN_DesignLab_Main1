package ai.rojan.designlab.components

import ai.rojan.designlab.ui.theme.RojanBlushPink
import ai.rojan.designlab.ui.theme.RojanOrbBlushGlow
import ai.rojan.designlab.ui.theme.RojanOrbLavenderGlow
import ai.rojan.designlab.ui.theme.RojanSoftLavender
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Design-system refinement, Phase 5 — [GlassOrb.kt]'s two `@Preview`
 * functions were the only raw `Color(0x…)` hex in this file (never a real
 * screen call site — `GlassOrb`/`FrostedGlassOrb`'s actual callers already
 * pass tokens). This pins the alias replacements: [RojanOrbLavenderGlow]
 * / [RojanOrbBlushGlow] must keep resolving to the existing approved
 * brand tones they alias, not drift into a new, unnamed color.
 */
class GlassOrbTokenTest {

    @Test
    fun `RojanOrbLavenderGlow aliases the existing RojanSoftLavender token`() {
        assertEquals(RojanSoftLavender, RojanOrbLavenderGlow)
    }

    @Test
    fun `RojanOrbBlushGlow aliases the existing RojanBlushPink token`() {
        assertEquals(RojanBlushPink, RojanOrbBlushGlow)
    }
}
