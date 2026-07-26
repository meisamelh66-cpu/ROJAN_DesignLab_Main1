package ai.rojan.designlab.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.components.brand.RojanLogo
import ai.rojan.designlab.components.brand.VersionFooter
import ai.rojan.designlab.components.roles.RoleRow
import ai.rojan.designlab.ui.background.PremiumBackground

/**
 * UX Refactor Phase 1: this screen is now a secondary entry point
 * (Manager/Specialist "business login," reached from Member Salons
 * List) rather than the app's default landing screen — a customer with
 * no persisted role now starts on Member Salons List instead. The
 * "Get Started" booking hero that used to live here was removed since
 * it led into the now-deleted category-first booking flow, and doesn't
 * fit a role-selection screen whose remaining audience is business
 * roles, not customers starting a booking.
 *
 * UX Refactor Phase 4: [onRoleSelected] no longer takes a role type —
 * both cards launch the same real phone/OTP login (see [RoleRow]'s doc
 * comment); which one was tapped stopped carrying any real meaning back
 * in Phase 3.
 */
@Composable
fun WelcomeScreen(
    onRoleSelected: () -> Unit,
) {

    PremiumBackground {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 20.dp,
                    vertical = 32.dp // Global Design System Refinement (Phase 1): 24dp -> 32dp, "increase breathing room... calmer and more premium"
                ),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.SpaceBetween
        ) {


            RojanLogo()



            RoleRow(
                onRoleSelected = onRoleSelected
            )



            VersionFooter()

        }
    }
}