package ai.rojan.designlab.ui.components.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

import ai.rojan.designlab.ui.theme.RojanPremiumBorderGold
import ai.rojan.designlab.ui.theme.RojanPremiumBorderHighlight
import ai.rojan.designlab.ui.theme.RojanPremiumBorderRoseGold
import ai.rojan.designlab.ui.theme.RojanPremiumBorderShadow
import ai.rojan.designlab.ui.theme.RojanPremiumBorderSpecular

/**
 * Premium Glass Border Design Update — polished Rose Gold + Gold metallic
 * edge, called by [ai.rojan.designlab.ui.components.glass.PremiumGlassSurface]
 * (the Shared Premium Glass Design System's canonical mechanic; every
 * app-facing glass surface is a thin wrapper around it) so the whole app
 * reads as one border language, per
 * design/reference/Premium_Glass_Border_Reference.png.
 *
 * Replaces the plain `Modifier.border(brush = Brush.linearGradient(...,
 * end = Offset(600f, 600f)))` used in the first two passes — that was the
 * actual bug behind "Rose Gold barely visible / border reads flat":
 * `Offset(600f, 600f)` is fixed *pixel* space, but most cards in this app
 * (a KPI tile, a quick-action chip) render at 200-450px, well short of
 * 600px. The gradient's later stops (Rose Gold, the second glint) never
 * got reached — every card only ever painted the gradient's first sliver,
 * near-solid Gold. Drawing with [drawWithContent] + `drawOutline` instead
 * uses `size.width`/`size.height` — the element's own real rendered size —
 * as the gradient end, so the full sweep always completes across every
 * card regardless of its dimensions.
 *
 * Refinement pass (matching the reference's polished-metal look, not just
 * its two brand hues): a flat two/three-tone sweep still reads as a thin
 * painted line, because real metal reads as metal only when it alternates
 * bright and shadowed passages. This adds [RojanPremiumBorderShadow] (a
 * darkened-gold contrast band) between every highlight, a distinct
 * near-white [RojanPremiumBorderSpecular] tone for the sharpest glint
 * points (separate from the broader champagne [RojanPremiumBorderHighlight]
 * band), a top-to-transparent sheen pass so the upper arcs read brighter
 * than the lower ones (light falling from above), a soft multi-stroke
 * outer glow faked via widening/fading passes (no BlurMaskFilter — it
 * silently no-ops on Compose's hardware-accelerated canvas), and glints at
 * two corners instead of one. [RojanPremiumBorderGold]/[RojanPremiumBorderRoseGold]
 * themselves are untouched — they're the exact hex values from the
 * reference's own palette swatch, so the fix is in composition/contrast,
 * not the base hues.
 */
fun Modifier.premiumMetallicBorder(
    shape: Shape,
    strokeWidth: Dp = 2.6.dp,
    baseAlpha: Float = 1f,
    secondaryAlpha: Float = 0.9f,
): Modifier = this.drawWithContent {
    drawContent()

    val strokePx = strokeWidth.toPx()
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }

    val gold = RojanPremiumBorderGold.copy(alpha = baseAlpha)
    val highlight = RojanPremiumBorderHighlight.copy(alpha = baseAlpha)
    val roseGold = RojanPremiumBorderRoseGold.copy(alpha = secondaryAlpha)
    val shadow = RojanPremiumBorderShadow.copy(alpha = baseAlpha)
    val specular = RojanPremiumBorderSpecular.copy(alpha = baseAlpha)

    // Soft outer glow — faked via several widening, fading passes of the
    // same path rather than a real blur (BlurMaskFilter doesn't render on
    // Compose's hardware-accelerated canvas). Using many small steps
    // instead of few large ones keeps each pass's hard stroke edge below
    // the threshold where it reads as a visible concentric ring/band —
    // more samples approximates a soft falloff instead of "stepping."
    // Sized off the element's own shorter side so the halo reads as a
    // genuine bloom at real screen density; clamped so tiny chips don't
    // get an oversized halo relative to their own size. Drawn first so
    // the crisp strokes below sit on top of it.
    val glowSpan = minOf(size.width, size.height)
    val maxGlowWidth = (glowSpan * 0.14f).coerceIn(strokePx * 4f, strokePx * 13f)
    val glowSteps = 7
    for (i in glowSteps downTo 1) {
        val t = i / glowSteps.toFloat()
        val width = (maxGlowWidth * t).coerceAtLeast(strokePx)
        val alpha = (0.05f + 0.05f * (1f - t)) * baseAlpha
        val tint = if (i % 2 == 0) roseGold else gold
        drawPath(path = path, color = tint.copy(alpha = alpha), style = Stroke(width = width))
    }

    // The metallic body — alternating shadow/gold/highlight/rose-gold
    // bands across the real element size, so even small chips show
    // multiple light/dark passages (what actually reads as "polished
    // metal") rather than one smooth flat-toned sweep. The specular band
    // (0.53-0.59) is kept narrow against its shadow neighbors so it reads
    // as one sharp flash of catching light, not a slow fade.
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to shadow,
                0.10f to gold,
                0.20f to highlight,
                0.32f to roseGold,
                0.44f to shadow,
                0.53f to shadow,
                0.56f to specular,
                0.59f to shadow,
                0.68f to roseGold,
                0.80f to gold,
                0.90f to highlight,
                1.00f to shadow,
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
        style = Stroke(width = strokePx),
    )

    // Directional sheen — light reading as if falling from above, so the
    // border's top arcs read brighter than its bottom ones ("edge
    // reflections" / premium depth), layered on top of the diagonal sweep.
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to specular.copy(alpha = 0.5f * baseAlpha),
                0.35f to specular.copy(alpha = 0f),
                1.00f to Color.Transparent,
            ),
        ),
        style = Stroke(width = strokePx),
    )

    // Corner sparkle, top-start — a literal 4-point star flash rather
    // than a soft radial smear, matching the reference's own sparkle
    // accents (the same glyph the AI Insight icon uses). A same-tone soft
    // bloom sits behind it so the star doesn't look pasted onto a plain
    // background, then a tiny bright core sells "catching light."
    val cornerInset = (glowSpan * 0.09f).coerceIn(strokePx * 3f, strokePx * 9f)
    val primarySparkleCenter = Offset(cornerInset, cornerInset)
    val primarySparkleSize = (glowSpan * 0.045f).coerceIn(strokePx * 2.5f, strokePx * 6f)
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(specular.copy(alpha = 0.55f * baseAlpha), specular.copy(alpha = 0f)),
            center = primarySparkleCenter,
            radius = primarySparkleSize * 3f,
        ),
        style = Stroke(width = strokePx),
    )
    drawSparkle(center = primarySparkleCenter, outerRadius = primarySparkleSize, color = specular)
    drawSparkle(center = primarySparkleCenter, outerRadius = primarySparkleSize * 0.32f, color = Color.White.copy(alpha = 0.9f * baseAlpha))

    // Secondary, smaller sparkle partway along the top edge — real
    // polished metal catches light at more than one point, but this one
    // stays quieter/smaller than the primary corner flash, not a twin.
    val secondarySparkleCenter = Offset(size.width * 0.62f, cornerInset * 0.6f)
    val secondarySparkleSize = primarySparkleSize * 0.55f
    drawSparkle(center = secondarySparkleCenter, outerRadius = secondarySparkleSize, color = highlight.copy(alpha = 0.85f * baseAlpha))
}

/** Classic 4-point sparkle/star: 8 vertices alternating outer/inner radius, tips on the axes. */
private fun DrawScope.drawSparkle(center: Offset, outerRadius: Float, color: Color) {
    if (outerRadius <= 0f) return
    val innerRadius = outerRadius * 0.28f
    val star = Path().apply {
        for (i in 0 until 8) {
            val angle = Math.PI / 4.0 * i
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val x = center.x + (radius * cos(angle)).toFloat()
            val y = center.y + (radius * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = star, color = color)
}
