package ai.rojan.designlab

import ai.rojan.designlab.manager.navigation.ManagerNavGraph
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
 * unmodified [ManagerNavGraph]/Manager screens/theme — nothing
 * duplicated or redesigned, just a new thin entry point in the
 * flavor-only `src/manager` source set.
 *
 * [ManagerNavGraph] (Manager Auth Flow Implementation) now owns both the
 * session-restore gate and the `NavHost` that used to be built directly
 * here — mirrors [MainActivity]'s own `setContent { RojanNavGraph() }`
 * shape exactly.
 */
class ManagerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalRojanPalette provides ManagerPalette) {
                RojanTheme {
                    ManagerNavGraph()
                }
            }
        }
    }
}
