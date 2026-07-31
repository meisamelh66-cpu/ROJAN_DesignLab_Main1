package ai.rojan.designlab.screens.customer.hometheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.glass.PremiumGlassSurface
import ai.rojan.designlab.ui.components.glass.PremiumGlassTheme
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.CustomerPalette
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.text.Text

/**
 * Customer Home dark-glass surface — Home Visual Language Unification,
 * then folded into the Shared Premium Glass Design System refactor: this
 * is now a thin, palette-bound wrapper around
 * [ai.rojan.designlab.ui.components.glass.PremiumGlassSurface] (the
 * canonical mechanic, Manager is its reference implementation) rather
 * than its own implementation. This drops the extra blurred ambient-glow
 * layer this file used to draw behind the glass, and the highlight's 0.4×
 * radius in favor of the canonical 0.35× — both were real "layering"
 * mechanic drift from Manager's, not just a color difference, and the
 * Shared Premium Glass Design System requires layering/glass-depth to be
 * mechanically identical across apps; only [CustomerPalette]'s tint
 * colors vary. [glassAlpha]/[glassSecondaryAlpha] params are kept for the
 * two real call-site overrides that exist (`SalonListScreen`'s selected/
 * unselected filter chip, `SalonDetailsScreen`'s explicit info-card
 * tuning) but now default to the canonical 0.14f/0.06f rather than this
 * file's old 0.12f/0.10f.
 */
@Composable
fun HomeGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    elevation: Dp = RojanShadows.FloatingElevation,
    showHighlight: Boolean = true,
    glassAlpha: Float = PremiumGlassTheme.FillAlpha,
    glassSecondaryAlpha: Float = PremiumGlassTheme.FillSecondaryAlpha,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalRojanPalette provides CustomerPalette) {
        PremiumGlassSurface(
            modifier = modifier,
            shape = shape,
            fillAlpha = glassAlpha,
            fillSecondaryAlpha = glassSecondaryAlpha,
            elevation = elevation,
            showHighlight = showHighlight,
            content = content,
        )
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
