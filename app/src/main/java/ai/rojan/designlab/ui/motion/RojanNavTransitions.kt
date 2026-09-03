package ai.rojan.designlab.ui.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn

/**
 * ROJAN page-transition set (UI Polish Sprint 2, Task 2).
 *
 * One definition of how screens arrive and leave, consumed by both
 * navigation graphs:
 * - Customer (`RojanNavGraph`) already had a fade + slight scale-in; that
 *   exact feel is preserved here as [pageEnter] / [pageExit] and now
 *   sourced from [RojanMotion] tokens instead of local literals.
 * - Manager (`ManagerRootGraph` / `managerNavGraph`) previously had **no**
 *   transitions (screens snapped in). It now uses the same set at the
 *   `NavHost` level.
 *
 * **Direction-neutral by design** — a fade plus a barely-perceptible
 * scale, no horizontal slide. This app is Persian-first but deliberately
 * renders in a fixed `LayoutDirection` (see `ui/text/RojanText.kt`), so a
 * left/right slide would need per-direction handling and could read
 * "backwards" in RTL. A centred fade+scale is unambiguous in either
 * reading direction — the safest "RTL compatible" choice and identical to
 * what Customer already shipped.
 *
 * Reduced motion: pass `reduceMotion = true` (from `rememberReducedMotion()`)
 * and callers get a short, scale-free cross-fade — enough to avoid a hard
 * cut, nothing that moves or zooms.
 *
 * This does **not** change navigation architecture: it only supplies the
 * `EnterTransition` / `ExitTransition` values the existing `composable {}`
 * / `NavHost {}` APIs already accept.
 */
object RojanNavTransitions {

    fun pageEnter(reduceMotion: Boolean = false): EnterTransition =
        if (reduceMotion) {
            fadeIn(animationSpec = RojanMotion.enterTween(RojanMotion.Quick))
        } else {
            fadeIn(animationSpec = RojanMotion.enterTween(RojanMotion.Standard)) +
                scaleIn(
                    initialScale = RojanMotion.PageScaleFrom,
                    animationSpec = RojanMotion.enterTween(RojanMotion.Standard),
                )
        }

    fun pageExit(reduceMotion: Boolean = false): ExitTransition =
        if (reduceMotion) {
            fadeOut(animationSpec = RojanMotion.exitTween(RojanMotion.Fast))
        } else {
            fadeOut(animationSpec = RojanMotion.exitTween(RojanMotion.StandardExit))
        }

    /**
     * Pop (back) transitions. Kept the same as forward for now — the
     * fade+scale reads fine in both directions and matches Customer's
     * prior behaviour, where `popEnter`/`popExit` were left to default to
     * `enter`/`exit`. Separated out so a distinct back-feel can be tuned
     * later without touching call sites.
     */
    fun popEnter(reduceMotion: Boolean = false): EnterTransition = pageEnter(reduceMotion)

    fun popExit(reduceMotion: Boolean = false): ExitTransition = pageExit(reduceMotion)
}
