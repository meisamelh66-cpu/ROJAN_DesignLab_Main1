package ai.rojan.designlab.navigation

import ai.rojan.designlab.screens.welcome.WelcomeScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

/**
 * UX Refactor Phase 4: this used to drive a role-selection save flow
 * (persist a Role, wait for it, show Saving/Error overlays) before
 * navigating. Tapping a card is now just an instant navigation to real
 * phone/OTP login (see RojanNavGraph's WELCOME composable and
 * [ai.rojan.designlab.ui.components.roles.RoleRow]'s doc comment) — there
 * is nothing left to await or fail, so this collapses to a thin wrapper.
 */
@Composable
fun WelcomeRoute(
    navController: NavController,
) {
    WelcomeScreen(
        onRoleSelected = {
            navController.navigate(RojanDestinations.AUTH)
        },
    )
}
