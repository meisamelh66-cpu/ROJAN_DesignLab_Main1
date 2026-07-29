package ai.rojan.designlab.ui.components.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ROJAN ambient glow — Final Premium Polish, Phase 1's "soft glow around
 * active elements" primitive. A soft radial color wash meant to sit
 * *behind* an element (an active tab, an AI-active affordance, a hero
 * accent) — distinct from [ai.rojan.designlab.ui.theme.RojanShadows]'
 * drop-shadows, which sit tight against a shape's own edge rather than
 * radiating outward from behind it.
 *
 * Deliberately not built on `ai.rojan.designlab.components.effects.Glow`'s
 * existing `GlowBox` — that file declares package
 * `ai.rojan.app.ui.components.effects` (a different root package than
 * this module's `ai.rojan.designlab`, not merely a different
 * subpackage), which is very likely dead/broken code. Extending a
 * mismatched-package file would compound the problem rather than fix
 * it; flagged as a Phase 3 consistency-audit finding instead.
 */
@Composable
fun RojanAmbientGlow(
    modifier: Modifier = Modifier,
    color: Color,
    alpha: Float = 0.35f,
    blurRadius: Dp = 24.dp,
    shape: Shape = CircleShape,
) {
    Box(
        modifier = modifier
            .blur(blurRadius)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                ),
                shape = shape,
            )
    )
}
