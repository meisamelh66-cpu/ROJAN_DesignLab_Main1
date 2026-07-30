package ai.rojan.designlab.screens.customer.hometheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.text.Text

/**
 * Customer Home dark-glass surface — Home Visual Language Unification.
 *
 * Same layered technique every glass primitive in this app already uses
 * (blurred ambient-glow layer behind a crisp bordered fill — see
 * [ai.rojan.designlab.screens.customer.theme.CustomerGlassSurface]), just
 * tuned for the dark reference: a light, faintly-purple frosted fill
 * (the shared `GlassSurface`'s white-based fill would read as a pale
 * smudge on a navy background) and a glowing purple border instead of a
 * hairline. The shared `GlassSurface`/`RojanHomeCard` files are not
 * modified — every Home section swaps onto this instead, the same way
 * `CustomerGlassSurface` sits alongside `GlassSurface` for Splash/Auth.
 */
@Composable
fun HomeGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    elevation: Dp = RojanShadows.FloatingElevation,
    showHighlight: Boolean = true,
    glassAlpha: Float = 0.12f,
    glassSecondaryAlpha: Float = 0.10f,
    content: @Composable () -> Unit,
) {
    Box {
        // Ambient glow behind the glass — purple/magenta, blurred,
        // unbounded so it diffuses past the card's own edges.
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(24.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            HomeColors.Glow.copy(alpha = 0.30f),
                            HomeColors.Magenta.copy(alpha = 0.22f),
                        ),
                    ),
                    shape = shape,
                ),
        )

        BoxWithConstraints(
            modifier = modifier
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    ambientColor = HomeColors.Glow.copy(alpha = 0.35f),
                    spotColor = HomeColors.Glow.copy(alpha = 0.30f),
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = glassAlpha),
                            HomeColors.Primary.copy(alpha = glassSecondaryAlpha),
                        ),
                    ),
                    shape = shape,
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            HomeColors.Glow.copy(alpha = 0.65f),
                            HomeColors.Primary.copy(alpha = 0.25f),
                        ),
                        start = Offset.Zero,
                        end = Offset(600f, 600f),
                    ),
                    shape = shape,
                ),
        ) {
            if (showHighlight) {
                val density = LocalDensity.current
                val highlightRadiusPx = with(density) {
                    (maxWidth.coerceAtLeast(maxHeight) * 0.4f).toPx()
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.10f),
                                    Color.Transparent,
                                ),
                                center = Offset.Zero,
                                radius = highlightRadiusPx.coerceAtLeast(1f),
                            ),
                            shape = shape,
                        ),
                )
            }

            content()
        }
    }
}

/**
 * Customer Home dark-glass card shell — same public shape as
 * [ai.rojan.designlab.ui.components.cards.RojanHomeCard] (accent tint,
 * press/entrance animation, index-staggered) so every Home section's card
 * call site is a like-for-like swap, just rendering through
 * [HomeGlassSurface] instead of the shared light `GlassSurface`.
 */
@Composable
fun HomeCard(
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = RojanDimens.CardWidthStandard,
    height: Dp = RojanDimens.CardHeightStandard,
    accentAlpha: Float = 0.22f,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    index: Int = 0,
    content: @Composable ColumnScope.() -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = modifier
            .width(width)
            .rojanEnterAnimation(visible = visible, delayMillis = index * 60),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(10.dp)
                .background(accentColor.copy(alpha = accentAlpha), RojanShapes.Small),
        )

        HomeGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = height)
                .rojanPressable(onClick = onClick),
            shape = RojanShapes.Small,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceSM),
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
                content = content,
            )
        }
    }
}

/** Dark-canvas counterpart to [ai.rojan.designlab.ui.components.cards.RojanRatingRow] — same "★ rating" content, [HomeColors] text tone instead of the light-surface token. */
@Composable
fun HomeRatingRow(rating: String, contentDescription: String = "امتیاز") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
    ) {
        RojanIconContainer(
            imageVector = Icons.Filled.Star,
            contentDescription = contentDescription,
            tint = HomeColors.Gold,
            size = RojanIconSize.Small,
        )
        Text(
            text = rating,
            style = RojanTypography.Caption,
            color = HomeColors.TextSecondary,
        )
    }
}
