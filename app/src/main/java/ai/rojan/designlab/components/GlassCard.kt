package ai.rojan.designlab.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.rojan.designlab.ui.theme.RojanBlushPink
import ai.rojan.designlab.ui.theme.RojanVividPurple

/**
 * Reusable premium frosted-glass card.
 *
 * Compose has no true backdrop blur pre-API 31, so "frosted glass" is
 * simulated the same way as the rest of this design system: a
 * semi-transparent gradient fill, a soft light border, a colored glow
 * layered behind the card, and a real elevation [shadow] for depth.
 *
 * Layering/sizing, in child declaration order:
 * 1. Glow layer — declared FIRST so it draws behind everything else.
 *    Uses [BoxScope.matchParentSize] to automatically track whatever
 *    size the surface layer below ends up being, then [Modifier.scale]
 *    enlarges it slightly so the glow visibly extends past the card's
 *    edges (scale only affects drawing, not layout/measurement, so this
 *    is safe — unlike negative padding, which Compose rejects at
 *    runtime), then blurred (real blur on API 31+, a soft unblurred halo
 *    below that — degrades gracefully).
 * 2. Glass surface layer — declared SECOND so it draws on top. This is a
 *    normally-sized Box (from its own [content] + [contentPadding]), and
 *    since it's the only child NOT using matchParentSize, it's what
 *    establishes the overall card size that the glow layer above tracks.
 *
 * This means GlassCard works correctly for both a large Hero Card and a
 * small RoleCard with no fixed dimensions required.
 *
 * @param glowColor tint of the soft ambient glow behind the card.
 * @param gradientColors the glass surface's own fill gradient.
 * @param elevation shadow elevation — exposed (rather than hardcoded) so
 * callers like [RoleCard] can drive it with an animated value for a
 * soft press response, without needing a second card implementation.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    contentPadding: Dp = 20.dp,
    gradientColors: List<Color> = listOf(
        Color.White.copy(alpha = 0.55f),
        RojanBlushPink.copy(alpha = 0.35f),
    ),
    borderColor: Color = Color.White.copy(alpha = 0.65f),
    glowColor: Color = RojanVividPurple.copy(alpha = 0.28f),
    elevation: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        // 1) Glow layer (behind) — sized to match the surface layer below.
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(1.08f)
                .blur(28.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(glowColor)
        )

        // 2) Glass surface layer (on top) — establishes the card's size.
        Box(
            modifier = Modifier
                .shadow(
                    elevation = elevation,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = glowColor,
                    spotColor = glowColor,
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(Brush.linearGradient(gradientColors))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(borderColor, Color.Transparent)),
                    shape = RoundedCornerShape(cornerRadius),
                )
                .padding(contentPadding),
            content = content,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDE3FF)
@Composable
private fun GlassCardPreview() {
    GlassCard(modifier = Modifier.size(280.dp, 160.dp)) {}
}