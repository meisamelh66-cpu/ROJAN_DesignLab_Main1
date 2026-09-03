package ai.rojan.designlab.ui.motion

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.theme.RojanShapes

/**
 * ROJAN AI motion primitives (UI Polish Sprint 2, Task 5) — the visual
 * vocabulary for "this surface is intelligent", as reusable, opt-in
 * modifiers. **No new AI features here** — these never decide what an AI
 * surface shows, only how an already-designated one breathes.
 *
 * Restraint is the point (Task 4/5: "no flashy effects", "luxury = calm"):
 * the glow is a slow, low-alpha pulse — nothing flashes, spins or jumps.
 * Every effect degrades to a quiet static state under reduced motion
 * (`reduceMotion` from `rememberReducedMotion()`), so the surface still
 * reads as special without any movement.
 */

/**
 * A soft brand-coloured glow that breathes behind [this] element — for an
 * AI insight card, an AI search entry, a Beauty-DNA result. Drawn behind
 * content and allowed to bleed a little past the element's edge.
 *
 * The caller supplies [color] (a brand accent — this primitive never picks
 * a colour). Under [reduceMotion] the glow holds still at [baseAlpha].
 */
fun Modifier.rojanAiGlow(
    color: Color,
    reduceMotion: Boolean,
    baseAlpha: Float = 0.10f,
    pulseDelta: Float = 0.05f,
): Modifier = composed {
    val alpha: Float = if (reduceMotion) {
        baseAlpha
    } else {
        val transition = rememberInfiniteTransition(label = "rojan_ai_glow")
        val v by transition.animateFloat(
            initialValue = (baseAlpha - pulseDelta).coerceAtLeast(0f),
            targetValue = (baseAlpha + pulseDelta).coerceAtMost(1f),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = RojanMotion.Ambient, easing = RojanMotion.EmphasisEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "rojan_ai_glow_alpha",
        )
        v
    }

    drawBehind {
        val outset = 12.dp.toPx()
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha.coerceIn(0f, 1f)), color.copy(alpha = 0f)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = maxOf(size.width, size.height) * 0.75f + outset,
            ),
            topLeft = Offset(-outset, -outset),
            size = Size(size.width + outset * 2f, size.height + outset * 2f),
        )
    }
}

/**
 * A very subtle moving sheen for a premium-glass hero / AI surface — the
 * glass slowly "catches light". Opt-in; never put this on every card
 * (Task 4: "subtle … highlight movement", "forbidden: flashy effects").
 * No-op under reduced motion. Does not touch the frozen `PremiumGlassSurface`
 * mechanic — it is an extra layer the caller adds.
 */
fun Modifier.rojanGlassSheen(
    reduceMotion: Boolean,
    tint: Color = Color.White,
): Modifier = composed {
    if (reduceMotion) return@composed this

    val transition = rememberInfiniteTransition(label = "rojan_glass_sheen")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = RojanMotion.Ambient * 2, easing = RojanMotion.EmphasisEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rojan_glass_sheen_phase",
    )

    drawBehind {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(tint.copy(alpha = 0.06f), tint.copy(alpha = 0f)),
                center = Offset(size.width * (0.25f + 0.5f * phase), size.height * 0.2f),
                radius = size.minDimension * 0.6f,
            ),
        )
    }
}

/**
 * A calm, brand-styled "AI is thinking" indicator — three dots that fade
 * in sequence. A deliberate alternative to a spinning progress ring for
 * AI-driven waits (insight generation, smart search). Under reduced motion
 * the dots simply render static at a resting alpha.
 */
@Composable
fun RojanAiThinkingIndicator(
    color: Color,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "rojan_ai_thinking")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            val a: Float = if (reduceMotion) {
                0.6f
            } else {
                val v by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = RojanMotion.Ambient / 4,
                            easing = RojanMotion.EmphasisEasing,
                        ),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(i * (RojanMotion.Ambient / 12)),
                    ),
                    label = "rojan_ai_thinking_dot_$i",
                )
                v
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(color.copy(alpha = a), RojanShapes.Circle),
            )
        }
    }
}
