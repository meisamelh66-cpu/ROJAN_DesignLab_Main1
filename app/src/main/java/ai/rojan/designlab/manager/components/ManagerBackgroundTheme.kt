package ai.rojan.designlab.manager.components

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
 * ROJAN Manager dark luxury background — replaces the shared
 * `WarmBackground` (light theme, still used by Customer/left untouched)
 * for the Manager app only. Deep teal/emerald gradient base with one
 * quiet turquoise glow (top-start) and one quiet gold glow (bottom-end)
 * — deliberately restrained ("calm... never noisy... keep cards
 * readable" per the approved reference), same two-zone technique
 * `WarmBackground` already uses, just re-themed with
 * [ManagerColors] instead of Customer's warm-white/purple palette.
 */
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
                    colors = listOf(
                        ManagerColors.BaseSecondary,
                        ManagerColors.BasePrimary,
                        ManagerColors.BaseDeep,
                    ),
                ),
            ),
    ) {
        val density = LocalDensity.current
        val glowRadiusPx = with(density) { (maxWidth.coerceAtLeast(maxHeight) * 0.8f).toPx() }

        // Quiet turquoise glow, top-start — the glass system's edge
        // light needs some ambient turquoise in the scene behind it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ManagerColors.Turquoise.copy(alpha = 0.16f),
                            ManagerColors.Turquoise.copy(alpha = 0f),
                        ),
                        center = Offset.Zero,
                        radius = glowRadiusPx,
                    ),
                ),
        )

        // Quiet gold glow, bottom-end — the "gold light accents" from
        // the reference, kept subtle so it stays a warm undertone
        // rather than a second competing bright zone.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ManagerColors.Gold.copy(alpha = 0.10f),
                            ManagerColors.Gold.copy(alpha = 0f),
                        ),
                        center = Offset(
                            with(density) { maxWidth.toPx() },
                            with(density) { maxHeight.toPx() },
                        ),
                        radius = glowRadiusPx,
                    ),
                ),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
