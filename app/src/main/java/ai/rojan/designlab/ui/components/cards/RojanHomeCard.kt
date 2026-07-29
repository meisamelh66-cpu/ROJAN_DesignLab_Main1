package ai.rojan.designlab.ui.components.cards

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.animation.rojanEnterAnimation
import ai.rojan.designlab.ui.components.glass.GlassSurface
import ai.rojan.designlab.ui.components.icon.RojanIconContainer
import ai.rojan.designlab.ui.components.icon.RojanIconSize
import ai.rojan.designlab.ui.components.interaction.rojanPressable
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShadows
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import ai.rojan.designlab.ui.theme.RojanTypography
import ai.rojan.designlab.ui.text.Text

/**
 * ROJAN Home card shell — Final Premium Polish, Phase 2 (Home).
 *
 * Extracted from six near-identical copies of the same
 * `Box(tinted background) > GlassSurface(clickable) > Column(padding,
 * spacedBy)` shell, independently duplicated across `FeaturedSalons`,
 * `NearbySalons`, `RecommendedSalons`, `FollowedSalons`, `RecentVisits`,
 * and `TopSpecialists` — each section's actual card *content* (which
 * fields it shows, image vs. avatar, AI badge, favorite pin) genuinely
 * differs and stays inline at each call site via [content]; only the
 * structural shell they all copy-pasted is consolidated here.
 *
 * Also where two premium-polish primitives now apply to every one of
 * those six sections at once, instead of being wired into each
 * separately: [index] drives a staggered fade + slight-upward-motion
 * entrance ([ai.rojan.designlab.ui.animation.rojanEnterAnimation]) so a
 * `LazyRow` of these settles in one card after another rather than
 * popping in all at once, and the card's tap target now goes through
 * [rojanPressable] (a subtle press-scale) instead of a plain
 * `Modifier.clickable {}` with no feedback at all.
 *
 * 3D Glassmorphism Depth Pass: layered depth added without touching this
 * card's footprint (`width`/`height` stay exactly what every caller
 * already lays out against, so no `LazyRow` measurement changes) —
 * [accentColor]'s tint is now blurred (a genuine background ambient-glow
 * layer, diffusing at the tint's own edges) sitting behind the crisp
 * [GlassSurface] (the existing glass-surface layer; its own
 * `showHighlight` already supplies the top-highlight/reflection, unchanged)
 * which now floats at [RojanShadows.PremiumElevation] instead of
 * [GlassSurface]'s default Floating tier, for a more pronounced "lifted
 * off the background" shadow.
 */
@Composable
fun RojanHomeCard(
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = RojanDimens.CardWidthStandard,
    height: Dp = RojanDimens.CardHeightStandard,
    accentAlpha: Float = 0.30f,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    index: Int = 0,
    content: @Composable ColumnScope.() -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Font-scale fix: was a hard `.size(width, height)` — at larger system
    // font sizes the Column's content (name/title/rating chip) needed more
    // vertical room than the fixed height allowed, so the rating chip got
    // clipped at the card's bottom edge. `heightIn(min = height)` keeps the
    // exact same height as before whenever content fits within it (the
    // default-font-scale case, byte-identical to the old fixed size), and
    // only grows the card when accessibility font scaling genuinely needs
    // more room — not a layout/design change, a font-scale-support fix.
    // The tint layer uses `matchParentSize()` (a Box-scope primitive built
    // for exactly this: a background sized off a content-driven sibling)
    // instead of `fillMaxSize()`, which needed a fixed-size parent.
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

        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = height)
                .rojanPressable(onClick = onClick),
            shape = RojanShapes.Small,
            elevation = RojanShadows.PremiumElevation,
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

/** Shared "★ rating" row — byte-identical across four of [RojanHomeCard]'s six current call sites before this extraction. */
@Composable
fun RojanRatingRow(rating: String, contentDescription: String = "امتیاز") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceXS),
    ) {
        RojanIconContainer(
            imageVector = Icons.Filled.Star,
            contentDescription = contentDescription,
            tint = RojanTextSecondary,
            size = RojanIconSize.Small,
        )
        Text(
            text = rating,
            style = RojanTypography.Caption,
            color = RojanTextSecondary,
        )
    }
}
