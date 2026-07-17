package ai.rojan.designlab.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


/**
 * ROJAN AI Shadow System — Design Token Specification v1.0/v1.1, Section 7.
 *
 * Corrected 4-tier hierarchy (Phase 3 Component 1 finalization pass):
 * previously, [SoftElevation] was named "Soft" but held the value the
 * spec actually defines as the "Floating" tier (18dp), and no true
 * "Soft" (8dp) or named "Premium" value existed. Verified before this
 * change that [SoftElevation] had zero callers anywhere in the codebase
 * (its old 18dp value is now [FloatingElevation]) — this rename/re-value
 * is safe with no risk of regressing an existing caller.
 */
object RojanShadows {

    /** Rojan.Shadow.Soft — small, resting-state elevation (cards at rest). Visual Refinement: +5% depth (8dp -> 8.4dp). */
    val SoftElevation: Dp = 8.4.dp

    /** Rojan.Shadow.Floating — medium, standard elevated-card elevation. This is the value [SoftElevation] incorrectly held before this fix. Visual Refinement: +5% depth (18dp -> 18.9dp). */
    val FloatingElevation: Dp = 18.9.dp

    /** Rojan.Shadow.Premium — hero/premium-level elevation. Visual Refinement: +5% depth (24dp -> 25.2dp). */
    val PremiumElevation: Dp = 25.2.dp


    val PremiumShadow =
        Color(
            0x66000000
        )


    /**
     * Rojan.Shadow.AIGlow — the AI-active ambient glow. Derived from
     * [RojanAIGlow] (RojanTokens.kt) rather than a separate hardcoded hex,
     * so the brand color and its glow can never drift apart independently.
     */
    val GlowPurple =
        RojanAIGlow.copy(alpha = 0.533f)


    val GlowPink =
        Color(
            0x88FF4081
        )


    val GlowRed =
        Color(
            0x88FF1744
        )
}
