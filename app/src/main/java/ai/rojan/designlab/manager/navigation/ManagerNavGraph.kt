package ai.rojan.designlab.manager.navigation

import ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel
import ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModelFactory
import ai.rojan.designlab.manager.screens.booking.ManagerBookingCustomerScreen
import ai.rojan.designlab.manager.screens.booking.ManagerBookingDateTimeScreen
import ai.rojan.designlab.manager.screens.booking.ManagerBookingReviewScreen
import ai.rojan.designlab.manager.screens.booking.ManagerBookingServiceScreen
import ai.rojan.designlab.manager.screens.booking.ManagerBookingSpecialistScreen
import ai.rojan.designlab.manager.screens.booking.ManagerBookingStartScreen
import ai.rojan.designlab.manager.screens.booking.ManagerBookingSuccessScreen
import ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen
import ai.rojan.designlab.manager.screens.customers.ManagerCustomerProfileScreen
import ai.rojan.designlab.manager.screens.customers.ManagerCustomersListScreen
import ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen
import ai.rojan.designlab.manager.screens.profile.ManagerProfileScreen
import ai.rojan.designlab.manager.screens.splash.ManagerSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument

/**
 * Self-contained nav graph for the isolated Manager App workspace.
 * Registers [ManagerDestinations.SPLASH], [ManagerDestinations.DASHBOARD],
 * [ManagerDestinations.CALENDAR], [ManagerDestinations.CUSTOMERS],
 * [ManagerDestinations.CUSTOMER_PROFILE], [ManagerDestinations.PROFILE],
 * and the [ManagerDestinations.BOOKING_FLOW_GRAPH] nested graph
 * (services/staff/settings are still foundation folders only, no
 * screens yet). This is the real entry graph for the separately
 * installable ROJAN Manager app (`ManagerActivity`, `manager` product
 * flavor) — the shared `RojanNavGraph.kt`/Customer app are untouched
 * and unaffected.
 *
 * Splash auto-advances to Dashboard and is removed from the back stack
 * (no "back to splash").
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
            onCreateAppointmentClick = { navController.navigate(ManagerDestinations.CREATE_APPOINTMENT) },
            onViewCustomersClick = { navController.navigate(ManagerDestinations.CUSTOMERS) },
            onProfileClick = { navController.navigate(ManagerDestinations.PROFILE) },
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

    composable(ManagerDestinations.PROFILE) {
        ManagerProfileScreen(
            onBackClick = { navController.popBackStack() },
        )
    }

    // Booking Journey — nested graph so every screen inside shares ONE
    // ManagerBookingViewModel instance, scoped to this graph's own
    // back-stack entry (mirrors Customer's RojanDestinations.BOOKING_FLOW_GRAPH
    // / bookingViewModelFor in RojanNavGraph.kt). Jetpack Navigation clears
    // that ViewModel's store automatically once this whole graph is popped.
    navigation(
        route = ManagerDestinations.BOOKING_FLOW_GRAPH,
        startDestination = ManagerDestinations.CREATE_APPOINTMENT,
    ) {
        composable(ManagerDestinations.CREATE_APPOINTMENT) { backStackEntry ->
            val viewModel = managerBookingViewModelFor(navController, backStackEntry)
            ManagerBookingStartScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onStartClick = { navController.navigate(ManagerDestinations.BOOKING_CUSTOMER) },
            )
        }

        composable(ManagerDestinations.BOOKING_CUSTOMER) { backStackEntry ->
            val viewModel = managerBookingViewModelFor(navController, backStackEntry)
            ManagerBookingCustomerScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onCustomerSelected = { navController.navigate(ManagerDestinations.BOOKING_SERVICE) },
            )
        }

        composable(ManagerDestinations.BOOKING_SERVICE) { backStackEntry ->
            val viewModel = managerBookingViewModelFor(navController, backStackEntry)
            ManagerBookingServiceScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onServiceSelected = { navController.navigate(ManagerDestinations.BOOKING_SPECIALIST) },
            )
        }

        composable(ManagerDestinations.BOOKING_SPECIALIST) { backStackEntry ->
            val viewModel = managerBookingViewModelFor(navController, backStackEntry)
            ManagerBookingSpecialistScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onSpecialistSelected = { navController.navigate(ManagerDestinations.BOOKING_DATETIME) },
            )
        }

        composable(ManagerDestinations.BOOKING_DATETIME) { backStackEntry ->
            val viewModel = managerBookingViewModelFor(navController, backStackEntry)
            ManagerBookingDateTimeScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onContinueClick = { navController.navigate(ManagerDestinations.BOOKING_REVIEW) },
            )
        }

        composable(ManagerDestinations.BOOKING_REVIEW) { backStackEntry ->
            val viewModel = managerBookingViewModelFor(navController, backStackEntry)
            ManagerBookingReviewScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onConfirmed = { navController.navigate(ManagerDestinations.BOOKING_SUCCESS) },
            )
        }

        composable(ManagerDestinations.BOOKING_SUCCESS) {
            ManagerBookingSuccessScreen(
                onDoneClick = {
                    // Pops the whole booking sub-graph at once — this is what
                    // clears ManagerBookingViewModel's state, not a manual
                    // reset call (mirrors BookingSuccessScreen's wiring in
                    // RojanNavGraph.kt).
                    navController.navigate(ManagerDestinations.DASHBOARD) {
                        popUpTo(ManagerDestinations.DASHBOARD) { inclusive = false }
                    }
                },
            )
        }
    }
}

/**
 * Obtains the single [ManagerBookingViewModel] instance shared by every
 * screen inside the [ManagerDestinations.BOOKING_FLOW_GRAPH] sub-graph,
 * scoped to that sub-graph's own back-stack entry rather than any
 * individual screen's — same pattern as Customer's `bookingViewModelFor`
 * in `RojanNavGraph.kt`.
 */
@Composable
private fun managerBookingViewModelFor(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
): ManagerBookingViewModel {
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(ManagerDestinations.BOOKING_FLOW_GRAPH)
    }
    return viewModel(
        viewModelStoreOwner = parentEntry,
        factory = ManagerBookingViewModelFactory(),
    )
}
