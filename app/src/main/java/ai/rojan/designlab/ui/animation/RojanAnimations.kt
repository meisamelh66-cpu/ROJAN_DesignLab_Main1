package ai.rojan.designlab.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


/**
 * ROJAN Motion System — Final Premium Polish, Phase 1.
 *
 * [PageEnter]/[PageExit] are the single source of truth for cross-screen
 * navigation transitions — extracted here, unchanged in value, from what
 * used to be private `motionEnter`/`motionExit` vals duplicated only
 * inside `RojanNavGraph.kt`. Identical tween timings/easing, so wiring
 * the nav graph onto these is a pure relocation, not a visual change.
 *
 * [ContentEnterSpec] backs [rojanEnterAnimation] below — the "screen
 * fade + slight upward motion" entrance for in-screen content (cards,
 * sections), which is a different concern from a full-screen nav
 * transition: it plays once when a piece of content first appears in a
 * still-visible screen (e.g. a card in a list), not when navigating
 * between screens.
 */
object RojanAnimations {


    val Enter =
        tween<Float>(
            durationMillis = 700,
            easing = FastOutSlowInEasing
        )


    val Glow =
        tween<Float>(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        )

    val PageEnter: EnterTransition =
        fadeIn(animationSpec = tween(280)) +
            scaleIn(initialScale = 0.97f, animationSpec = tween(280))

    val PageExit: ExitTransition =
        fadeOut(animationSpec = tween(160))

    val ContentEnterSpec =
        tween<Float>(
            durationMillis = 420,
            easing = FastOutSlowInEasing,
        )

    /** Default vertical travel for [rojanEnterAnimation]'s "slight upward motion". */
    val ContentEnterTravel: Dp = 16.dp
}


@Composable
fun rememberScaleAnimation(
    visible:Boolean
):Float{


    return animateFloatAsState(

        targetValue =
            if(visible)
                1f
            else
                0.85f,

        animationSpec =
            RojanAnimations.Enter

    ).value

}

/**
 * Content-entrance hook — was a fade + slight-upward-motion animation
 * (delay via [delayMillis], then an alpha/translationY ramp), staggered
 * per list item (e.g. `index * 60ms` across a `LazyRow`/`LazyColumn`).
 *
 * Scroll-recycle bug fix (Icon/Card Delayed-Appearance Investigation):
 * every real call site of this modifier lives *inside* a lazy list item.
 * Compose's `LazyColumn`/`LazyRow` disposes an item's composition once it
 * scrolls far enough outside the retained window, and recomposes it from
 * scratch when scrolled back into view — which restarted this modifier's
 * `remember`-backed `started` flag at its initial value and replayed the
 * full delay+fade sequence on every such recycle. That read as icons/cards
 * "popping in late" on ordinary up/down scrolling, not just on a screen's
 * true first appearance, which is never correct — a scrolled-into-view
 * item was already part of the loaded screen, not new content arriving.
 *
 * Fixed by removing the delay/fade mechanism entirely: content is now
 * always fully visible (same end state — alpha 1, zero translation —
 * that the animation always settled on), so there is no longer a replay
 * to trigger regardless of how lazy-list recycling behaves. [visible]/
 * [delayMillis] stay as no-op parameters so no call site (~20 across the
 * app) needs to change.
 */
@Composable
fun Modifier.rojanEnterAnimation(
    visible: Boolean = true,
    delayMillis: Int = 0,
): Modifier = this