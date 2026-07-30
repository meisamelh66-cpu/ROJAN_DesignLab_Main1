package ai.rojan.designlab.screens.customer.theme

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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ROJAN Customer icon theme — a single reusable wrapper
 * ([CustomerIconContainer]) for a feminine-luxury glass icon family
 * (Explore/Search/Favorites/Appointments/Messages/Profile/Beauty
 * services), matching [CustomerIconTheme]'s pink/lavender/rose-gold
 * language. Foundation only — not wired into any existing Customer
 * icon call site yet (`RojanIconContainer` and every screen using it
 * directly are untouched). Reuses [RojanIconContainer] for the actual
 * icon rendering rather than duplicating that logic — only the
 * surrounding container styling is new.
 */
object CustomerIconTheme {
    val ContainerSize: Dp = 44.dp
    val GlowSizeMultiplier: Float = 1.5f
}

@Composable
fun CustomerIconContainer(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: RojanIconSize = RojanIconSize.Medium,
    containerSize: Dp = CustomerIconTheme.ContainerSize,
) {
    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center,
    ) {
        // Soft pink glow behind the glass circle.
        Box(
            modifier = Modifier
                .size(containerSize * CustomerIconTheme.GlowSizeMultiplier)
                .blur(16.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CustomerColors.Rose.copy(alpha = 0.30f),
                            CustomerColors.Rose.copy(alpha = 0f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        Box(
            modifier = Modifier
                .size(containerSize)
                .shadow(
                    elevation = RojanShadows.SoftElevation,
                    shape = CircleShape,
                    ambientColor = CustomerColors.RoseGold.copy(alpha = 0.25f),
                    spotColor = Color.Black.copy(alpha = 0.10f),
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CustomerColors.BlushPink.copy(alpha = 0.35f),
                            CustomerColors.SoftLavender.copy(alpha = 0.35f),
                        ),
                    ),
                    shape = CircleShape,
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CustomerColors.RoseGold.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.35f),
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
                tint = CustomerColors.TextPrimary,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 120, heightDp = 120)
@Composable
private fun CustomerIconContainerPreview() {
    CustomerIconContainer(
        imageVector = Icons.Filled.Favorite,
        contentDescription = "علاقه‌مندی‌ها",
    )
}
