package ai.rojan.designlab.manager.navigation

import ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen
import ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen
import ai.rojan.designlab.manager.screens.splash.ManagerSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Self-contained nav graph for the isolated Manager App workspace.
 * Registers [ManagerDestinations.SPLASH], [ManagerDestinations.DASHBOARD],
 * and [ManagerDestinations.CALENDAR] (customers/services/staff/settings
 * are still foundation folders only, no screens yet). This is now the
 * real entry graph for the separately installable ROJAN Manager app
 * (`ManagerActivity`, `manager` product flavor) — the shared
 * `RojanNavGraph.kt`/Customer app are untouched and unaffected.
 *
 * Dashboard's Calendar Preview "مشاهده تقویم کامل" CTA is wired to
 * [ManagerDestinations.CALENDAR]; Splash auto-advances to Dashboard and
 * is removed from the back stack (no "back to splash").
 */
fun NavGraphBuilder.managerNavGraph(navController: NavController) {
    composable(ManagerDestinations.SPLASH) {
        ManagerSplashScreen(
            onSplashFinished = {
                navController.navigate(ManagerDestinations.DASHBOARD) {
                    popUpTo(ManagerDestinations.SPLASH) { inclusive = true }
                }
            },
        )
    }

    composable(ManagerDestinations.DASHBOARD) {
        ManagerDashboardScreen(
            onViewCalendarClick = { navController.navigate(ManagerDestinations.CALENDAR) },
        )
    }

    composable(ManagerDestinations.CALENDAR) {
        ManagerCalendarScreen(
            onBackClick = { navController.popBackStack() },
        )
    }
}
