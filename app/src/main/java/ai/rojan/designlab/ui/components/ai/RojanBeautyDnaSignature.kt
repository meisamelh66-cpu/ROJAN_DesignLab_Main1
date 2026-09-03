package ai.rojan.designlab.ui.components.ai

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

import ai.rojan.designlab.ui.motion.RojanMotion
import ai.rojan.designlab.ui.motion.rememberReducedMotion
import ai.rojan.designlab.ui.theme.LocalRojanPalette

/**
 * One facet of a Beauty-DNA signature — a caller-supplied visual weight
 * and colour.
 *
 * **The component never derives these.** A future Beauty-DNA screen maps
 * the customer's own explicit profile selections (hair / skin / nail
 * preferences they entered) to facets. This type carries no attribute
 * name, no category, no inference — only "how prominent" and "what
 * colour", both decided by the caller. See
 * `docs/uiux/ROJAN_AI_UI_GOVERNANCE_v1.md`.
 */
data class RojanDnaFacet(
    /** Relative prominence, 0f..1f. */
    val weight: Float,
    /** A brand-palette colour the caller chose for this facet. */
    val color: Color,
)

/**
 * ROJAN Beauty-DNA visual signature (UI Polish Sprint 4, Task 3) —
 * **visualization only**.
 *
 * Draws a calm, deterministic radial "bloom" from the supplied [facets]:
 * one soft petal per facet, spaced evenly, its reach set by the facet's
 * weight and its tint by the facet's colour, over a faint web to a
 * central core. The same facets always produce the same signature.
 *
 * With **no facets** it renders a neutral dashed ring — an honest "not
 * set up yet" placeholder, never a fabricated pattern implying the app
 * knows something about the customer.
 *
 * Motion: a single grow-in from the centre on first appearance
 * (scroll-safe latch), then static. No infinite animation. Reduced motion
 * → appears instantly.
 */
@Composable
fun RojanBeautyDnaSignature(
    facets: List<RojanDnaFacet>,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    label: String = "امضای بصری بیوتی دی‌ان‌ای",
) {
    val palette = LocalRojanPalette.current
    val reduceMotion = rememberReducedMotion()

    var appeared by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val reveal by animateFloatAsState(
        targetValue = if (appeared || reduceMotion) 1f else 0f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else RojanMotion.Reveal, easing = RojanMotion.EmphasisEasing),
        label = "rojan_dna_reveal",
    )

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = label },
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val maxR = this.size.minDimension / 2f

        if (facets.isEmpty()) {
            drawCircle(
                color = palette.textSecondary.copy(alpha = 0.35f * reveal),
                radius = maxR * 0.7f,
                center = center,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
                ),
            )
            drawCircle(color = palette.textSecondary.copy(alpha = 0.20f * reveal), radius = maxR * 0.08f, center = center)
            return@Canvas
        }

        // Faint web + central core.
        val n = facets.size
        facets.forEachIndexed { i, facet ->
            val angle = (i.toFloat() / n) * 2f * Math.PI.toFloat() - (Math.PI.toFloat() / 2f)
            val w = facet.weight.coerceIn(0f, 1f)
            val dist = maxR * (0.30f + 0.55f * w) * reveal
            val petalCenter = Offset(
                center.x + dist * cos(angle),
                center.y + dist * sin(angle),
            )
            val petalR = maxR * (0.16f + 0.14f * w) * reveal

            drawLine(
                color = facet.color.copy(alpha = 0.18f * reveal),
                start = center,
                end = petalCenter,
                strokeWidth = 1.5.dp.toPx(),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(facet.color.copy(alpha = 0.55f * reveal), facet.color.copy(alpha = 0f)),
                    center = petalCenter,
                    radius = petalR.coerceAtLeast(1f),
                ),
                radius = petalR.coerceAtLeast(1f),
                center = petalCenter,
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.textAccent.copy(alpha = 0.9f * reveal), palette.textAccent.copy(alpha = 0.1f * reveal)),
                center = center,
                radius = maxR * 0.14f,
            ),
            radius = maxR * 0.14f,
            center = center,
        )
    }
}
