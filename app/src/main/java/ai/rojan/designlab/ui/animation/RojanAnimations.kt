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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay


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
 * Fade + slight-upward-motion entrance for one piece of content — the
 * shared primitive behind "smooth card appearance" / staggered list
 * entrances across the app. [delayMillis] staggers multiple items (e.g.
 * an index * 60ms stagger across a `LazyRow` of cards) so they settle in
 * one after another instead of all popping in at once; 0 (the default)
 * plays immediately. Purely visual — never gates real content loading,
 * so it's safe to apply to already-loaded data without changing when
 * that data becomes interactive.
 */
@Composable
fun Modifier.rojanEnterAnimation(
    visible: Boolean = true,
    delayMillis: Int = 0,
): Modifier {
    var started by remember { mutableStateOf(delayMillis <= 0) }

    LaunchedEffect(visible) {
        if (visible && delayMillis > 0) {
            delay(delayMillis.toLong())
            started = true
        } else if (visible) {
            started = true
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (visible && started) 1f else 0f,
        animationSpec = RojanAnimations.ContentEnterSpec,
        label = "rojan_content_enter",
    )

    return this
        .alpha(progress)
        .graphicsLayer {
            translationY = (1f - progress) * RojanAnimations.ContentEnterTravel.toPx()
        }
}