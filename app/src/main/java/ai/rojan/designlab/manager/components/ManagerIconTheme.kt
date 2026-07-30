package ai.rojan.designlab.manager.components

import ai.rojan.designlab.ui.components.effects.RojanAmbientGlow
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.theme.RojanShadows
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ROJAN Manager icon theme — a single reusable wrapper
 * ([ManagerIconContainer]) that makes any *existing* Material icon read
 * as part of the new dark-luxury Manager theme, until real bespoke 3D
 * icon assets are introduced later. Deliberately not a set of
 * individual custom icons and not fake "final" 3D icons — one shared,
 * reused container, so swapping in real assets later is a one-file
 * change, not a per-screen rewrite.
 *
 * Visual language (from `design/reference/ROJAN_Manager_Reference.png`):
 * soft ambient glow behind a circular glass surface (accent-to-deep-teal
 * gradient fill, thin accent→gold gradient rim as the "gold accent
 * reflection"), icon itself tinted with the accent. Reuses the existing
 * [RojanAmbientGlow]/[RojanIconContainer] primitives rather than
 * duplicating their logic — only the container styling is new.
 *
 * [accentColor] defaults to Turquoise (the primary Manager accent) but
 * is overridable per call site — the reference itself alternates
 * teal-tinted and gold-tinted icon circles across the KPI row (booking/
 * customers vs. revenue/occupancy), so this stays faithful to that
 * rather than flattening every icon to one identical hue. The
 * container's shape, size, glow technique, and lighting direction never
 * change — only the accent hue does.
 */
object ManagerIconTheme {
    val ContainerSize: Dp = 48.dp
    val GlowSizeMultiplier: Float = 1.6f
}

@Composable
fun ManagerIconContainer(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: RojanIconSize = RojanIconSize.Medium,
    containerSize: Dp = ManagerIconTheme.ContainerSize,
    accentColor: Color = ManagerColors.Turquoise,
) {
    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center,
    ) {
        RojanAmbientGlow(
            modifier = Modifier.size(containerSize * ManagerIconTheme.GlowSizeMultiplier),
            color = accentColor,
            alpha = 0.35f,
        )

        Box(
            modifier = Modifier
                .size(containerSize)
                .shadow(
                    elevation = RojanShadows.SoftElevation,
                    shape = CircleShape,
                    ambientColor = accentColor.copy(alpha = 0.35f),
                    spotColor = ManagerColors.BaseDeep.copy(alpha = 0.6f),
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.30f),
                            ManagerColors.BaseSecondary.copy(alpha = 0.40f),
                        ),
                    ),
                    shape = CircleShape,
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.65f),
                            ManagerColors.Gold.copy(alpha = 0.40f),
                        ),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            RojanIconContainer(
                imageVector = imageVector,
                contentDescription = contentDescription,
                size = size,
                tint = if (accentColor == ManagerColors.Gold) ManagerColors.GoldLight else ManagerColors.TurquoiseLight,
            )
        }
    }
}
