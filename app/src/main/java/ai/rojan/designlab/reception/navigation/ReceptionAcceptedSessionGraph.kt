package ai.rojan.designlab.reception.navigation

import ai.rojan.designlab.reception.presentation.booking.ReceptionBookingViewModel
import ai.rojan.designlab.reception.presentation.booking.ReceptionBookingViewModelFactory
import ai.rojan.designlab.reception.presentation.customers.ReceptionCustomersViewModel
import ai.rojan.designlab.reception.presentation.customers.ReceptionCustomersViewModelFactory
import ai.rojan.designlab.reception.presentation.dashboard.ReceptionDashboardViewModel
import ai.rojan.designlab.reception.presentation.dashboard.ReceptionDashboardViewModelFactory
import ai.rojan.designlab.reception.screens.booking.ReceptionBookingCustomerScreen
import ai.rojan.designlab.reception.screens.booking.ReceptionBookingDateTimeScreen
import ai.rojan.designlab.reception.screens.booking.ReceptionBookingReviewScreen
import ai.rojan.designlab.reception.screens.booking.ReceptionBookingServiceScreen
import ai.rojan.designlab.reception.screens.booking.ReceptionBookingSpecialistScreen
import ai.rojan.designlab.reception.screens.booking.ReceptionBookingStartScreen
import ai.rojan.designlab.reception.screens.booking.ReceptionBookingSuccessScreen
import ai.rojan.designlab.reception.screens.customers.ReceptionCustomersListScreen
import ai.rojan.designlab.reception.screens.dashboard.ReceptionDashboardScreen
import ai.rojan.designlab.reception.screens.profile.ReceptionProfileScreen
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController

/**
 * P1-1, Reception Invite Acceptance Flow: the real, working post-acceptance
 * session — [salonId] is already resolved (by
 * [ai.rojan.designlab.reception.presentation.invite.ReceptionInviteAcceptanceViewModel],
 * via a fresh `/users/me/salon-access` fetch), so every screen here is
 * constructed directly from it, the same salonId-direct factory shape
 * [ReceptionDestinations.DASHBOARD]/[ReceptionDestinations.CUSTOMERS]/the
 * booking wizard already use in [receptionNavGraph] — **without** a
 * [ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel]
 * instance, which this flow never constructs (see
 * [ai.rojan.designlab.reception.presentation.invite.ReceptionInviteAcceptanceViewModel]'s
 * own doc comment for why). Reuses every existing screen/ViewModel/factory
 * as-is — this file is only new *wiring*, not new screen logic — so
 * Dashboard/Customers/Profile/booking behave identically to the normal
 * gated app once a session is real.
 *
 * A separate, small [NavHost]/[NavController] from [receptionNavGraph]'s —
 * deliberately not merged into that graph, since merging would require
 * [ai.rojan.designlab.reception.navigation.ReceptionRootGraph]'s existing
 * gate to construct a real [ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel]
 * for this session too, which is exactly the interaction this whole flow
 * exists to avoid (that ViewModel's role gate would reject the still-`CUSTOMER`-role
 * account this flow just onboarded).
 *
 * [onLogoutClick] on [ReceptionProfileScreen] closes the app rather than
 * routing to [ReceptionDestinations.OTP_AUTH] (a route only [receptionNavGraph]
 * — not this graph — registers): the session persisted here (tokens +
 * personId, saved by the invite flow's own OTP step) is self-healing on
 * the next cold start regardless, since [ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel.restoreSession]
 * already clears any session whose account isn't `MANAGER`-role — a real,
 * disclosed limitation of the currently-approved architecture (a
 * newly-onboarded receptionist's session lives only for this app-open),
 * not something this file works around.
 */
@Composable
fun ReceptionAcceptedSessionGraph(
    salonId: String,
    fullName: String,
    phoneNumber: String?,
) {
    val navController = rememberNavController()
    val activity = LocalContext.current as? Activity

    NavHost(navController = navController, startDestination = ReceptionDestinations.DASHBOARD) {
        composable(ReceptionDestinations.DASHBOARD) {
            val appContext = LocalContext.current.applicationContext
            val dashboardViewModel: ReceptionDashboardViewModel = viewModel(
                factory = ReceptionDashboardViewModelFactory(appContext, salonId),
            )
            ReceptionDashboardScreen(
                viewModel = dashboardViewModel,
                onNewBookingClick = { navController.navigate(ReceptionDestinations.BOOKING_FLOW_GRAPH) },
                onCustomersClick = { navController.navigate(ReceptionDestinations.CUSTOMERS) },
                onProfileClick = { navController.navigate(ReceptionDestinations.PROFILE) },
            )
        }

        composable(ReceptionDestinations.CUSTOMERS) {
            val appContext = LocalContext.current.applicationContext
            val customersViewModel: ReceptionCustomersViewModel = viewModel(
                factory = ReceptionCustomersViewModelFactory(appContext, salonId),
            )
            ReceptionCustomersListScreen(
                viewModel = customersViewModel,
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(ReceptionDestinations.PROFILE) {
            ReceptionProfileScreen(
                fullName = fullName,
                phoneNumber = phoneNumber,
                onBackClick = { navController.popBackStack() },
                onLogoutClick = { activity?.finish() },
            )
        }

        navigation(
            route = ReceptionDestinations.BOOKING_FLOW_GRAPH,
            startDestination = ReceptionDestinations.CREATE_APPOINTMENT,
        ) {
            composable(ReceptionDestinations.CREATE_APPOINTMENT) { backStackEntry ->
                val viewModel = acceptedSessionBookingViewModelFor(navController, backStackEntry, salonId)
                ReceptionBookingStartScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onStartClick = { navController.navigate(ReceptionDestinations.BOOKING_CUSTOMER) },
                )
            }

            composable(ReceptionDestinations.BOOKING_CUSTOMER) { backStackEntry ->
                val viewModel = acceptedSessionBookingViewModelFor(navController, backStackEntry, salonId)
                ReceptionBookingCustomerScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onCustomerSelected = { navController.navigate(ReceptionDestinations.BOOKING_SERVICE) },
                )
            }

            composable(ReceptionDestinations.BOOKING_SERVICE) { backStackEntry ->
                val viewModel = acceptedSessionBookingViewModelFor(navController, backStackEntry, salonId)
                ReceptionBookingServiceScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onServiceSelected = { navController.navigate(ReceptionDestinations.BOOKING_SPECIALIST) },
                )
            }

            composable(ReceptionDestinations.BOOKING_SPECIALIST) { backStackEntry ->
                val viewModel = acceptedSessionBookingViewModelFor(navController, backStackEntry, salonId)
                ReceptionBookingSpecialistScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onSpecialistSelected = { navController.navigate(ReceptionDestinations.BOOKING_DATETIME) },
                )
            }

            composable(ReceptionDestinations.BOOKING_DATETIME) { backStackEntry ->
                val viewModel = acceptedSessionBookingViewModelFor(navController, backStackEntry, salonId)
                ReceptionBookingDateTimeScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onContinueClick = { navController.navigate(ReceptionDestinations.BOOKING_REVIEW) },
                )
            }

            composable(ReceptionDestinations.BOOKING_REVIEW) { backStackEntry ->
                val viewModel = acceptedSessionBookingViewModelFor(navController, backStackEntry, salonId)
                ReceptionBookingReviewScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onConfirmed = { navController.navigate(ReceptionDestinations.BOOKING_SUCCESS) },
                )
            }

            composable(ReceptionDestinations.BOOKING_SUCCESS) {
                ReceptionBookingSuccessScreen(
                    onDoneClick = {
                        navController.navigate(ReceptionDestinations.DASHBOARD) {
                            popUpTo(ReceptionDestinations.DASHBOARD) { inclusive = false }
                        }
                    },
                )
            }
        }
    }
}

/** Same "one shared ViewModel per booking sub-graph back-stack entry" shape as [receptionBookingViewModelFor] in `ReceptionNavGraph.kt` — [salonId]-direct rather than [ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel]-resolved, since this graph never constructs that ViewModel. */
@Composable
private fun acceptedSessionBookingViewModelFor(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    salonId: String,
): ReceptionBookingViewModel {
    val appContext = LocalContext.current.applicationContext
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(ReceptionDestinations.BOOKING_FLOW_GRAPH)
    }
    return viewModel(
        viewModelStoreOwner = parentEntry,
        factory = ReceptionBookingViewModelFactory(appContext, salonId),
    )
}
