package ai.rojan.designlab

import ai.rojan.designlab.manager.navigation.ManagerDestinations
import ai.rojan.designlab.manager.navigation.managerNavGraph
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.ManagerPalette
import ai.rojan.designlab.ui.theme.RojanTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

/**
 * ROJAN Manager app's launcher entry point — the `manager` product
 * flavor's counterpart to Customer's [MainActivity] (which this flavor
 * removes via `src/manager/AndroidManifest.xml`). Reuses the existing,
 * unmodified [managerNavGraph]/Manager screens/theme — nothing
 * duplicated or redesigned, just a new thin entry point in the
 * flavor-only `src/manager` source set.
 */
class ManagerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalRojanPalette provides ManagerPalette) {
                RojanTheme {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = ManagerDestinations.SPLASH,
                    ) {
                        managerNavGraph(navController)
                    }
                }
            }
        }
    }
}
