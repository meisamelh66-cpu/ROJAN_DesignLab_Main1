package ai.rojan.designlab.manager.navigation

import ai.rojan.designlab.manager.presentation.auth.ManagerAuthViewModel
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
import ai.rojan.designlab.manager.screens.auth.ManagerOtpAuthScreen
import ai.rojan.designlab.manager.screens.auth.ManagerSalonSelectionScreen
import ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen
import ai.rojan.designlab.manager.screens.profile.ManagerProfileScreen
import ai.rojan.designlab.manager.screens.services.ManagerServicesScreen
import ai.rojan.designlab.manager.screens.staff.ManagerStaffEditScreen
import ai.rojan.designlab.manager.screens.staff.ManagerStaffScreen
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
 * Registers [ManagerDestinations.OTP_AUTH], [ManagerDestinations.SALON_SELECTION]
 * (Active Salon Context & Selection Flow), [ManagerDestinations.DASHBOARD],
 * [ManagerDestinations.CALENDAR], [ManagerDestinations.CUSTOMERS],
 * [ManagerDestinations.CUSTOMER_PROFILE], [ManagerDestinations.SERVICES]
 * (Manager Operational Foundation, Phase 6 Step 1), [ManagerDestinations.STAFF]/
 * [ManagerDestinations.STAFF_EDIT] (Phase 6 Step 2), [ManagerDestinations.PROFILE],
 * and the [ManagerDestinations.BOOKING_FLOW_GRAPH] nested graph
 * (settings is still a foundation folder only, no screen yet). This is
 * the real entry graph for the separately
 * installable ROJAN Manager app (`ManagerActivity`, `manager` product
 * flavor) — the shared `RojanNavGraph.kt`/Customer app are untouched
 * and unaffected.
 *
 * OTP Authentication Entry Flow Integration: splash is no longer a route
 * in this graph — it's shown by `ManagerRootGraph.kt`, the composable that
 * wraps this NavHost, before the NavHost (and therefore its
 * `startDestination`) is even created, so real session validation can
 * decide between [ManagerDestinations.DASHBOARD] and
 * [ManagerDestinations.OTP_AUTH] up front instead of always starting at a
 * splash route and redirecting afterward. [authViewModel] is threaded
 * through from that same wrapper (constructed once, not per-screen) so the
 * OTP screen and the Profile screen's logout affordance share one instance
 * with the gate itself.
 */
fun NavGraphBuilder.managerNavGraph(navController: NavController, authViewModel: ManagerAuthViewModel) {
    composable(ManagerDestinations.OTP_AUTH) {
        ManagerOtpAuthScreen(
            viewModel = authViewModel,
            onAuthenticated = {
                navController.navigate(ManagerDestinations.DASHBOARD) {
                    popUpTo(ManagerDestinations.OTP_AUTH) { inclusive = true }
                }
            },
        )
    }

    composable(ManagerDestinations.SALON_SELECTION) {
        ManagerSalonSelectionScreen(
            viewModel = authViewModel,
            onSalonSelected = {
                navController.navigate(ManagerDestinations.DASHBOARD) {
                    popUpTo(ManagerDestinations.SALON_SELECTION) { inclusive = true }
                }
            },
        )
    }

    composable(ManagerDestinations.DASHBOARD) {
        ManagerDashboardScreen(
            onViewCalendarClick = { navController.navigate(ManagerDestinations.CALENDAR) },
            onCreateAppointmentClick = { navController.navigate(ManagerDestinations.CREATE_APPOINTMENT) },
            onViewCustomersClick = { navController.navigate(ManagerDestinations.CUSTOMERS) },
            onViewServicesClick = { navController.navigate(ManagerDestinations.SERVICES) },
            onViewStaffClick = { navController.navigate(ManagerDestinations.STAFF) },
            onProfileClick = { navController.navigate(ManagerDestinations.PROFILE) },
        )
    }

    composable(ManagerDestinations.SERVICES) {
        ManagerServicesScreen(
            onBackClick = { navController.popBackStack() },
        )
    }

    composable(ManagerDestinations.STAFF) {
        ManagerStaffScreen(
            onBackClick = { navController.popBackStack() },
            onAddClick = { navController.navigate(ManagerDestinations.staffEdit(ManagerDestinations.NEW_SPECIALIST_ID)) },
            onSpecialistClick = { specialistId -> navController.navigate(ManagerDestinations.staffEdit(specialistId)) },
        )
    }

    composable(
        route = ManagerDestinations.STAFF_EDIT,
        arguments = listOf(navArgument("specialistId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val specialistId = backStackEntry.arguments?.getString("specialistId") ?: ManagerDestinations.NEW_SPECIALIST_ID
        ManagerStaffEditScreen(
            specialistId = specialistId,
            onBackClick = { navController.popBackStack() },
            onSaved = { navController.popBackStack() },
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
            onLogoutClick = {
                authViewModel.logout()
                navController.navigate(ManagerDestinations.OTP_AUTH) {
                    popUpTo(ManagerDestinations.DASHBOARD) { inclusive = true }
                }
            },
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
