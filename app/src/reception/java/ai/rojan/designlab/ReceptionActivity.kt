package ai.rojan.designlab

import ai.rojan.designlab.reception.navigation.ReceptionRootGraph
import ai.rojan.designlab.ui.theme.LocalRojanPalette
import ai.rojan.designlab.ui.theme.ReceptionPalette
import ai.rojan.designlab.ui.theme.RojanTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider

/**
 * ROJAN Reception app's launcher entry point — the `reception` product
 * flavor's counterpart to Customer's [MainActivity] and Manager's
 * [ManagerActivity] (both left untouched; this flavor removes MainActivity
 * via `src/reception/AndroidManifest.xml`, and does not touch Manager's
 * flavor-only source set at all). Same shape as [ManagerActivity]: no
 * JWT/session/OTP logic lives in this Activity itself, only theming setup
 * and a handoff to [ReceptionRootGraph].
 *
 * See ROJAN_Reception_Implementation_Plan_v1.md, Phase 0.
 */
class ReceptionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalRojanPalette provides ReceptionPalette) {
                RojanTheme {
                    ReceptionRootGraph()
                }
            }
        }
    }
}
