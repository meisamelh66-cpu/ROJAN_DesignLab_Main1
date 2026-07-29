package ai.rojan.designlab.ui.background

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity

import ai.rojan.designlab.ui.theme.RojanAIGlow
import ai.rojan.designlab.ui.theme.RojanBackgroundGradient
import ai.rojan.designlab.ui.theme.RojanWarmWhite

/**
 * Customer Journey UI Alignment — the approved Warm White background,
 * used from Member Salons List onward (every screen except Splash and
 * Welcome, which keep [PremiumBackground]'s decorative salon photo).
 *
 * Solid [RojanWarmWhite] base with the existing, already design-approved
 * [RojanBackgroundGradient] token (Warm White → Blush Pink → Aqua Mint →
 * Soft Lavender → Warm White) laid over it at reduced opacity — a soft
 * pastel undertone rather than a full-strength rainbow wash, so it reads
 * as "warm white" first. The same [RojanAIGlow] radial accent
 * [PremiumBackground] uses sits on top too, at a gentler alpha, so glass
 * cards/gradients/shadows still read as the same premium system — only
 * the busy decorative photo is gone.
 *
 * Depth Pass V2 (Background Atmosphere): the single top-start [RojanAIGlow]
 * zone gained a quiet [RojanBlushPink] counterpart anchored at the
 * opposite (bottom-end) corner — "soft ambient light zones" (plural),
 * still at a gentle alpha so the effect stays a calm undertone, not a
 * second bright accent; both use the exact same radial-gradient technique
 * already established just above, not a new lighting system.
 *
 * Luxury Visual Refinement Phase (Calm the Background): per audit
 * feedback that the screen had too many pastel colors competing at
 * once, [RojanBackgroundGradient]'s wash is pulled back hard (0.35f ->
 * 0.14f — it's a 5-hue cycle, the single biggest source of that
 * competition) and the second (bottom-end blush) glow zone added in
 * Depth Pass V2 is removed — one quiet [RojanAIGlow] zone remains,
 * slightly softer (0.06f -> 0.05f). Net effect: a calmer, closer-to-
 * neutral warm-white canvas that still carries the ROJAN purple/pink
 * identity as an undertone, not a competing accent.
 */
@Composable
fun WarmBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(RojanWarmWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = RojanBackgroundGradient.map { it.copy(alpha = 0.14f) }
                    )
                )
        )

        val density = LocalDensity.current
        val glowRadiusPx = with(density) { (maxWidth.coerceAtLeast(maxHeight) * 0.9f).toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            RojanAIGlow.copy(alpha = 0.05f),
                            RojanAIGlow.copy(alpha = 0f)
                        ),
                        center = Offset.Zero,
                        radius = glowRadiusPx
                    )
                )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
