package ai.rojan.designlab.ui.window

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/**
 * Sprint 5A-3 — the single place edge-to-edge is turned on for a ROJAN
 * launcher Activity ([ai.rojan.designlab.MainActivity],
 * [ai.rojan.designlab.ManagerActivity], [ai.rojan.designlab.ReceptionActivity]).
 *
 * `targetSdk = 37` means Android 15+ already lays the app out behind the
 * system bars whether we opt in or not; calling [enableEdgeToEdge] makes
 * that explicit on every API level AND — the reason a bare
 * `enableEdgeToEdge()` isn't enough — pins the status / navigation bar
 * icon contrast to the app's actual canvas. `enableEdgeToEdge()`'s
 * default (`SystemBarStyle.auto`) follows the *system* light/dark
 * setting, so a ROJAN dark canvas on a light-mode phone would get dark,
 * invisible status-bar icons.
 *
 * The bars themselves are transparent — the ROJAN background gradients
 * (`HomeBackgroundTheme` / `ManagerBackgroundTheme` / `WarmBackground`)
 * paint behind them, so full-bleed backgrounds are preserved. Content
 * inset spacing is applied by the screen scaffolds, never here.
 *
 * @param darkCanvas `true` for the Customer (navy / deep-purple) and
 *   Manager (emerald / teal) apps → light bar icons. `false` for
 *   Reception (`WarmBackground`, light) → dark bar icons.
 */
fun ComponentActivity.applyRojanEdgeToEdge(darkCanvas: Boolean) {
    val barStyle = if (darkCanvas) {
        SystemBarStyle.dark(scrim = Color.TRANSPARENT)
    } else {
        SystemBarStyle.light(scrim = Color.TRANSPARENT, darkScrim = DARK_ICON_SCRIM_FALLBACK)
    }
    enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
}

/**
 * Applied only on API levels where the light style can't render dark
 * status-bar icons (< API 23 for status, < API 26 for navigation) — a
 * translucent dark wash keeps the icons legible over the light Reception
 * canvas. Value matches AndroidX's own default `darkScrim`.
 */
private val DARK_ICON_SCRIM_FALLBACK = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
