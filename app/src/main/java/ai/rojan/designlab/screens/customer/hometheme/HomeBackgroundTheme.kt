package ai.rojan.designlab.screens.customer.hometheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity

/**
 * Customer Home dark-luxury background — Home Visual Language Unification.
 *
 * Used only by `CustomerHomeScreen`; does not replace or modify
 * [ai.rojan.designlab.ui.background.WarmBackground] (every other Customer
 * screen keeps that frozen light background untouched). Same layered
 * technique as [ai.rojan.designlab.ui.background.WarmBackground]/
 * [ai.rojan.designlab.screens.customer.theme.CustomerBackgroundTheme] —
 * solid base + gradient wash + radial glow zones — just a dark navy/purple
 * palette per the approved reference image instead of warm white.
 */
@Composable
fun HomeBackgroundTheme(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(HomeColors.NavyBase),
    ) {
        // Vertical navy -> deep purple wash, the base atmosphere.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(HomeColors.NavyBase, HomeColors.DeepPurple, HomeColors.NavyBase),
                    ),
                ),
        )

        val density = LocalDensity.current
        val glowRadiusPx = with(density) { (maxWidth.coerceAtLeast(maxHeight) * 0.9f).toPx() }

        // Soft purple glow, top-start.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(HomeColors.Glow.copy(alpha = 0.20f), HomeColors.Glow.copy(alpha = 0f)),
                        center = Offset.Zero,
                        radius = glowRadiusPx,
                    ),
                ),
        )

        // Quiet magenta/rose echo, bottom-end.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(HomeColors.Magenta.copy(alpha = 0.10f), HomeColors.Magenta.copy(alpha = 0f)),
                        center = Offset(
                            with(density) { maxWidth.toPx() },
                            with(density) { maxHeight.toPx() },
                        ),
                        radius = glowRadiusPx * 0.8f,
                    ),
                ),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
