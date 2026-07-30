package ai.rojan.designlab.manager.navigation

import ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen
import ai.rojan.designlab.manager.screens.customers.ManagerCustomerProfileScreen
import ai.rojan.designlab.manager.screens.customers.ManagerCustomersListScreen
import ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen
import ai.rojan.designlab.manager.screens.splash.ManagerSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * Self-contained nav graph for the isolated Manager App workspace.
 * Registers [ManagerDestinations.SPLASH], [ManagerDestinations.DASHBOARD],
 * [ManagerDestinations.CALENDAR], [ManagerDestinations.CUSTOMERS], and
 * [ManagerDestinations.CUSTOMER_PROFILE] (services/staff/settings are
 * still foundation folders only, no screens yet). This is the real
 * entry graph for the separately installable ROJAN Manager app
 * (`ManagerActivity`, `manager` product flavor) — the shared
 * `RojanNavGraph.kt`/Customer app are untouched and unaffected.
 *
 * Dashboard's Calendar Preview "مشاهده تقویم کامل" CTA is wired to
 * [ManagerDestinations.CALENDAR]; Splash auto-advances to Dashboard and
 * is removed from the back stack (no "back to splash"). Dashboard's own
 * composable registration is otherwise unchanged from the frozen
 * baseline — Customers isn't wired to any Dashboard entry point in this
 * pass (Dashboard/QuickActionsSection stay untouched).
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

    composable(ManagerDestinations.CUSTOMERS) {
        ManagerCustomersListScreen(
            onBackClick = { navController.popBackStack() },
            onCustomerClick = { customerId ->
                navController.navigate(ManagerDestinations.customerProfile(customerId))
            },
        )
    }

    composable(
        route = ManagerDestinations.CUSTOMER_PROFILE,
        arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val customerId = backStackEntry.arguments?.getString("customerId") ?: "c1"
        ManagerCustomerProfileScreen(
            onBackClick = { navController.popBackStack() },
            customerId = customerId,
        )
    }
}
