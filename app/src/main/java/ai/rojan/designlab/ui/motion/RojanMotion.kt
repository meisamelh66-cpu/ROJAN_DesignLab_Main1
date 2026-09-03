package ai.rojan.designlab.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ROJAN Motion System — the **single source of truth** for every
 * animation timing, curve, distance and spring in the app (UI Polish
 * Sprint 2). Nothing outside this file should hand-write a `tween(300)` or
 * a raw easing; screens and components consume named roles from here so
 * the whole motion feel can be retuned from one place.
 *
 * Design intent: **luxury = calm motion.** Durations are deliberately on
 * the restrained side, curves settle rather than bounce, distances are
 * small. No "gaming" motion — no big slides, no flashy overshoot on
 * ordinary content.
 *
 * Values here either match what the codebase already used (page
 * transitions were `280` / `160`; content reveal `420`; press `150`;
 * shimmer `1100`) or fill a gap that was previously an ad-hoc literal.
 * Existing rendered behaviour for non-reduced-motion users is unchanged
 * by introducing this layer.
 *
 * Reduced-motion: this object holds *values only*. The gate that turns
 * animation off lives in `ReducedMotion.kt` (`rememberReducedMotion()`);
 * motion utilities consult it and fall back to [Instant] / no travel.
 */
object RojanMotion {

    // ---- Duration scale (milliseconds) ----------------------------------

    /** No animation — the reduced-motion / instant target. */
    const val Instant: Int = 0

    /** Micro-interactions: press feedback, toggles, small tint swaps. (Matches the existing `rojanPressable` 150ms.) */
    const val Fast: Int = 150

    /** Small state changes: a chip selecting, an icon crossfade, a caption appearing. */
    const val Quick: Int = 220

    /** Default for page transitions and ordinary content reveal. (Matches the existing page-enter 280ms.) */
    const val Standard: Int = 280

    /** Exit half of a transition — shorter than [Standard] ("exit faster than enter"). (Matches the existing page-exit 160ms.) */
    const val StandardExit: Int = 160

    /** Deliberate reveals: a card settling into place, a glass surface arriving. (Matches the existing content-reveal 420ms.) */
    const val Reveal: Int = 420

    /** Hero-level, one-shot emphasis only (a success moment). Use sparingly. */
    const val Slow: Int = 600

    /** Breathing / ambient loops (AI glow, optional glass-highlight drift). One full cycle. */
    const val Ambient: Int = 4200

    /** Skeleton shimmer sweep — one pass. (Matches the existing 1100ms.) */
    const val Shimmer: Int = 1100

    /** Indeterminate loading-bar sweep — one pass. (Matches the existing `PremiumLoadingBar` 2500ms.) */
    const val LoadingSweep: Int = 2500

    // ---- Easing curves -------------------------------------------------

    /** Standard entrance/among-content curve — decelerate into place. */
    val EnterEasing: Easing = FastOutSlowInEasing

    /** Exit curve — accelerate away. */
    val ExitEasing: Easing = FastOutLinearInEasing

    /** Premium "settle" curve for reveals — a soft, confident deceleration (no overshoot). */
    val EmphasisEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    // ---- Spatial ------------------------------------------------------

    /** Upward travel for a content-reveal entrance (card / section). Small on purpose. */
    val ContentRise: Dp = 8.dp

    /** Page-transition horizontal slide, as a fraction of the container width. Tiny — a hint of spatial direction, not a full slide. */
    const val PageSlideFraction: Float = 0.04f

    /** Milliseconds between staggered siblings when a caller explicitly opts into a stagger. */
    const val StaggerStep: Int = 45

    // ---- Scale ------------------------------------------------------

    /** Page-transition incoming scale (scales up to 1f). Matches the existing 0.97f. */
    const val PageScaleFrom: Float = 0.97f

    /**
     * Press-feedback scale target used by `rojanPressable`. Named here so
     * it stops being a bare literal. **Value unchanged** from the frozen
     * baseline (`CLAUDE.md`: "rojanPressable (scale to 1.06f, 150ms)") —
     * Sprint 2 does not alter press behaviour; changing the direction of
     * this scale is a separate, ratified change (see the Sprint 2 report).
     */
    const val PressScale: Float = 1.06f

    // ---- Ready-made specs ------------------------------------------------

    /** Standard tween for a given duration + the standard enter easing. */
    fun <T> enterTween(durationMillis: Int = Standard): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = EnterEasing)

    /** Exit tween. */
    fun <T> exitTween(durationMillis: Int = StandardExit): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = ExitEasing)

    /** Reveal tween — the premium settle. */
    fun <T> revealTween(durationMillis: Int = Reveal): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = EmphasisEasing)

    /** Soft, non-bouncy spring for reveals/success that should feel physical but calm. */
    fun <T> gentleSpring(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
}
