package ai.rojan.designlab.manager.navigation

import ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen
import ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Self-contained nav graph for the isolated Manager App workspace.
 * Registers [ManagerDestinations.DASHBOARD] and, now, [ManagerDestinations.CALENDAR]
 * (customers/services/staff/settings are still foundation folders only,
 * no screens yet). Not called from the app's shared `RojanNavGraph.kt` —
 * hooking this into the real app entry point is a separate, later step.
 *
 * Dashboard's Calendar Preview "مشاهده تقویم کامل" CTA is wired here to
 * navigate to [ManagerDestinations.CALENDAR] — the only connection this
 * pass makes; every other Manager destination stays foundation-only.
 */
fun NavGraphBuilder.managerNavGraph(navController: NavController) {
    composable(ManagerDestinations.DASHBOARD) {
        ManagerDashboardScreen(
            onBackClick = { navController.popBackStack() },
            onViewCalendarClick = { navController.navigate(ManagerDestinations.CALENDAR) },
        )
    }

    composable(ManagerDestinations.CALENDAR) {
        ManagerCalendarScreen(
            onBackClick = { navController.popBackStack() },
        )
    }
}
