package ai.rojan.designlab.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.ui.motion.RojanMotion
import ai.rojan.designlab.ui.motion.RojanNavTransitions
import ai.rojan.designlab.ui.motion.rememberReducedMotion

/**
 * ROJAN Motion System — legacy holder, now a thin layer over
 * [ai.rojan.designlab.ui.motion.RojanMotion] (the single source of truth
 * for timings/curves, added in UI Polish Sprint 2) and
 * [RojanNavTransitions] (page transitions).
 *
 * Every constant below either delegates to `RojanMotion` or preserves its
 * exact prior value where a call site's feel must not change. Kept as a
 * type so existing imports (`RojanAnimations.PageEnter`,
 * `RojanAnimations.ContentEnterSpec`, …) keep resolving.
 */
object RojanAnimations {

    /** Legacy scale-in spec used by [rememberScaleAnimation]. Unchanged (700ms). */
    val Enter = tween<Float>(durationMillis = 700, easing = FastOutSlowInEasing)

    /** Legacy glow spec. Unchanged (1500ms). */
    val Glow = tween<Float>(durationMillis = 1500, easing = FastOutSlowInEasing)

    /**
     * Cross-screen navigation enter transition. Now sourced from
     * [RojanNavTransitions.pageEnter] (fade + slight scale-in) — same feel
     * as before, one definition shared with the Manager graph.
     * Reduced-motion is applied at the call site in the nav graphs, which
     * have a composable scope to read the setting; this default is the
     * full-motion variant.
     */
    val PageEnter: EnterTransition = RojanNavTransitions.pageEnter(reduceMotion = false)

    /** Cross-screen navigation exit transition. See [PageEnter]. */
    val PageExit: ExitTransition = RojanNavTransitions.pageExit(reduceMotion = false)

    /**
     * In-screen content settle spec (used by `SpecialistAvatar`,
     * `SearchModeTabs`). Value + easing preserved (420ms, FastOutSlowIn) —
     * only the literal is now named via [RojanMotion.Reveal].
     */
    val ContentEnterSpec = tween<Float>(durationMillis = RojanMotion.Reveal, easing = FastOutSlowInEasing)

    /** Default vertical travel for legacy content settle. Unchanged (16dp). */
    val ContentEnterTravel: Dp = 16.dp
}

@Composable
fun rememberScaleAnimation(visible: Boolean): Float {
    return animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = RojanAnimations.Enter,
        label = "rojan_scale_animation",
    ).value
}

/**
 * Content-entrance modifier — a calm fade + small upward settle the first
 * time a piece of content appears.
 *
 * **History / why this is careful.** An earlier version added a
 * `delayMillis`-then-fade with a per-item stagger and was removed
 * entirely (left as `Modifier = this`) because, inside a `LazyColumn`/
 * `LazyRow`, Compose disposes an item's composition when it scrolls out
 * of the retained window and recomposes it from scratch on the way back —
 * which replayed the whole delayed fade on ordinary scrolling ("cards
 * popping in late").
 *
 * **This version is scroll-safe:**
 * - The "already appeared" latch is [rememberSaveable]. `LazyColumn`/
 *   `LazyRow` give every item its own `SaveableStateHolder`, so for a
 *   keyed list or a fixed `item { }` block the latch survives recycling
 *   and the animation plays **exactly once**.
 * - There is **no delay** and **no stagger**. For an unkeyed `items {}`
 *   row whose slot identity genuinely changes, the worst case is a single
 *   ~420ms fade with an 8dp settle — a soft refresh, not the "late pop"
 *   glitch that caused the removal.
 * - It is a **no-op under reduced motion** (`rememberReducedMotion()`).
 * - It uses `graphicsLayer` only (alpha + translationY) — draw-time,
 *   zero layout impact, no reflow of siblings.
 *
 * [visible] / [delayMillis] are retained as accepted-but-ignored
 * parameters so the ~20 existing call sites compile unchanged; the
 * stagger `delayMillis` is intentionally not honoured (calm luxury +
 * scroll safety).
 */
@Composable
fun Modifier.rojanEnterAnimation(
    @Suppress("UNUSED_PARAMETER") visible: Boolean = true,
    @Suppress("UNUSED_PARAMETER") delayMillis: Int = 0,
): Modifier {
    if (rememberReducedMotion()) return this

    var appeared by rememberSaveable { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = RojanMotion.Reveal, easing = RojanMotion.EmphasisEasing),
        label = "rojan_enter_animation",
    )
    LaunchedEffect(Unit) { appeared = true }

    val riseDp = RojanMotion.ContentRise
    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * riseDp.toPx()
    }
}
