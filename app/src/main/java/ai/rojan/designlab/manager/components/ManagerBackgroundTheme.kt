package ai.rojan.designlab.manager.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

/**
 * ROJAN Manager dashboard background — pixel-perfect pass
 * (`rojan-ui-pixel-perfect` skill, `reference.png`): the canvas is
 * true/near-black, matching the reference exactly — the page itself
 * (including the gaps between cards) reads as flat black, with no
 * teal/gold wash across it. [nearBlackTop]/[nearBlackBottom] are
 * near-identical near-black literals (not [ManagerColors] tokens — that
 * file is outside this pass's approved scope) giving only the faintest
 * depth, never a visible teal or gold tint on the page background
 * itself. The one quiet turquoise glow kept here is deliberately much
 * fainter than before and sits only in the top-start corner (roughly
 * behind where the Hero renders) — restrained ambient light, not a
 * page-wide wash; the previous gold background glow is removed
 * entirely, since gold is accent-only per the reference (CTAs/primary
 * actions), never a background color.
 */
private val nearBlackTop = Color(0xFF050706)
private val nearBlackBottom = Color(0xFF000000)

@Composable
fun ManagerBackgroundTheme(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(nearBlackTop, nearBlackBottom),
                ),
            ),
    ) {
        val density = LocalDensity.current
        val glowRadiusPx = with(density) { (maxWidth.coerceAtLeast(maxHeight) * 0.6f).toPx() }

        // Restrained turquoise glow, top-start only — much fainter than
        // before, so the page reads as black first and foremost.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ManagerColors.Turquoise.copy(alpha = 0.06f),
                            ManagerColors.Turquoise.copy(alpha = 0f),
                        ),
                        center = Offset.Zero,
                        radius = glowRadiusPx,
                    ),
                ),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
