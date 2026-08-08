package ai.rojan.designlab

import ai.rojan.designlab.manager.navigation.ManagerRootGraph
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.ManagerPalette
import ai.rojan.designlab.ui.theme.RojanTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider

/**
 * ROJAN Manager app's launcher entry point — the `manager` product
 * flavor's counterpart to Customer's [MainActivity] (which this flavor
 * removes via `src/manager/AndroidManifest.xml`). Reuses the existing,
 * unmodified Manager screens/theme — nothing duplicated or redesigned,
 * just a thin entry point in the flavor-only `src/manager` source set.
 *
 * OTP Authentication Entry Flow Integration: delegates to
 * [ManagerRootGraph] instead of building a raw `NavHost` directly — no
 * JWT/session/OTP logic lives in this Activity itself (matching this
 * integration's own "do not place JWT handling inside Activity"
 * requirement); this class only sets up theming and hands off.
 */
class ManagerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalRojanPalette provides ManagerPalette) {
                RojanTheme {
                    ManagerRootGraph()
                }
            }
        }
    }
}
