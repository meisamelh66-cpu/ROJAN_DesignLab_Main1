package ai.rojan.designlab.ui.components.loading

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanLoadingGlowEnd
import ai.rojan.designlab.ui.theme.RojanLoadingGlowMid
import ai.rojan.designlab.ui.theme.RojanLoadingGlowStart
import ai.rojan.designlab.ui.theme.RojanLoadingTrack
import ai.rojan.designlab.ui.theme.RojanShapes

/**
 * ROJAN Shimmer/Skeleton system — Final Premium Polish, Phase 1's
 * "Loading & shimmer components" / "Skeleton components". Reuses the
 * Loading System color tokens already reserved in `RojanTokens.kt`
 * ([RojanLoadingTrack]/[RojanLoadingGlowStart]/[RojanLoadingGlowMid]/
 * [RojanLoadingGlowEnd]) — previously only wired into
 * [ai.rojan.designlab.components.PremiumLoadingBar]'s thin progress
 * sweep — extended here to full content-shaped placeholders, so a
 * loading list reads as "the same premium loading language, shaped like
 * the content it's replacing" rather than a generic spinner.
 *
 * A single diagonal gradient band sweeps across the shape on an
 * infinite loop; the rest of the shape stays at [RojanLoadingTrack] the
 * whole time (both gradient endpoints are that same color), so nothing
 * needs a separate base-fill layer underneath it.
 */
fun Modifier.rojanShimmer(shape: Shape = RojanShapes.Small): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "rojan_shimmer")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rojan_shimmer_sweep",
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            RojanLoadingTrack,
            RojanLoadingGlowStart,
            RojanLoadingGlowMid,
            RojanLoadingGlowEnd,
            RojanLoadingTrack,
        ),
        start = Offset(x = -600f + sweep * 1200f, y = 0f),
        end = Offset(x = -200f + sweep * 1200f, y = 400f),
    )

    this
        .clip(shape)
        .background(brush = brush, shape = shape)
}

/** A single shimmering placeholder shape — the smallest reusable unit (a skeleton "line" or "block"). */
@Composable
fun RojanSkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RojanShapes.Small,
) {
    Box(modifier = modifier.rojanShimmer(shape))
}

/**
 * Skeleton matching the standard horizontal-scroll salon/service card
 * size ([RojanDimens.CardWidthStandard] × [CardHeightStandard]) that
 * `FeaturedSalons`/`NearbySalons`/`RecommendedSalons`/etc. all already
 * share — one skeleton shape covers every one of those sections' loading
 * state rather than each screen inventing its own.
 */
@Composable
fun RojanCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.size(
            width = RojanDimens.CardWidthStandard,
            height = RojanDimens.CardHeightStandard,
        ),
        verticalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
    ) {
        RojanSkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
        )
        RojanSkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(14.dp),
        )
        RojanSkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .height(12.dp),
        )
    }
}
