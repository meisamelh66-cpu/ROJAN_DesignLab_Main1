package ai.rojan.designlab.ui.motion

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * ROJAN reduced-motion support (UI Polish Sprint 2, Task 6).
 *
 * Android has no direct "prefers reduced motion" flag before very recent
 * versions; the long-standing, widely-honoured proxy is the system
 * **animator duration scale** (Settings → Developer options / Accessibility
 * → "Remove animations"). When it is `0`, the user has asked for no
 * animation and the OS itself already zeroes most framework animations —
 * ROJAN's custom Compose animations must follow suit.
 *
 * [LocalReducedMotionOverride] lets a future in-app "Reduce motion" toggle
 * force the same behaviour without depending on the system setting. It is
 * `null` today (no toggle wired yet); when non-null it wins.
 *
 * Usage: `val reduce = rememberReducedMotion()` in a composable, then pass
 * `reduce` into motion utilities (or branch on it — reduced ⇒ no infinite
 * loops, no travel/scale, instant or very short cross-fades only).
 * **No information may depend on animation** — a reduced-motion user must
 * see the same end state, just without the transition.
 */
val LocalReducedMotionOverride = compositionLocalOf<Boolean?> { null }

/**
 * `true` when animation should be suppressed: an explicit
 * [LocalReducedMotionOverride], or the system animator scale being `0`.
 *
 * Read once per composition and remembered — the system setting does not
 * change mid-frame, and re-reading `Settings.Global` on every recomposition
 * would be wasteful.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    LocalReducedMotionOverride.current?.let { return it }

    val context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        scale == 0f
    }
}
