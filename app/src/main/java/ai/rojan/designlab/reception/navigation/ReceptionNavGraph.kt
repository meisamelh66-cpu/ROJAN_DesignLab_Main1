package ai.rojan.designlab.reception.navigation

import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.reception.domain.auth.ActiveSalonUiState
import ai.rojan.designlab.reception.domain.auth.ReceptionAuthState
import ai.rojan.designlab.reception.presentation.auth.ReceptionAuthViewModel
import ai.rojan.designlab.reception.presentation.booking.ReceptionBookingViewModel
import ai.rojan.designlab.reception.presentation.booking.ReceptionBookingViewModelFactory
import ai.rojan.designlab.reception.presentation.customers.ReceptionCustomersViewModel
import ai.rojan.designlab.reception.presentation.customers.ReceptionCustomersViewModelFactory
import ai.rojan.designlab.reception.presentation.dashboard.ReceptionDashboardViewModel
import ai.rojan.designlab.reception.presentation.dashboard.ReceptionDashboardViewModelFactory
import ai.rojan.designlab.reception.screens.auth.ReceptionAccessErrorScreen
import ai.rojan.designlab.reception.screens.auth.ReceptionOtpAuthScreen
import ai.rojan.designlab.reception.screens.auth.ReceptionSalonSelectionScreen
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation

/**
 * Self-contained nav graph for the isolated Reception App workspace.
 * Phase 0: auth + salon selection + a placeholder dashboard. Phase 1
 * (authentication completion): [ReceptionDestinations.ACCESS_ERROR] +
 * [ReceptionDestinations.PROFILE]. This phase (System2 Reception Phase1
 * Controlled Implementation): the real [ReceptionDestinations.DASHBOARD],
 * [ReceptionDestinations.CUSTOMERS], and the
 * [ReceptionDestinations.BOOKING_FLOW_GRAPH] nested graph. No Invite
 * routes — [ai.rojan.designlab.reception.domain.repository.ReceptionInviteRepository]
 * is an integration placeholder only (backend doesn't exist yet), so there
 * is deliberately no UI built against it.
 *
 * [authViewModel] is threaded through from [ReceptionRootGraph]
 * (constructed once, not per-screen). Every screen past [ReceptionDestinations.DASHBOARD]
 * needs a resolved `salonId`, obtained via [requireActiveSalonId] — safe
 * by construction, since every such screen is only ever reachable after
 * [ai.rojan.designlab.reception.domain.auth.ActiveSalonUiState.Active] has
 * already resolved (this graph's own [ReceptionDestinations.OTP_AUTH]/
 * [ReceptionDestinations.SALON_SELECTION]/[ReceptionDestinations.ACCESS_ERROR]
 * routes only ever navigate to [ReceptionDestinations.DASHBOARD] once that
 * holds).
 */
fun NavGraphBuilder.receptionNavGraph(navController: NavController, authViewModel: ReceptionAuthViewModel) {
    composable(ReceptionDestinations.OTP_AUTH) {
        ReceptionOtpAuthScreen(
            viewModel = authViewModel,
            onAuthenticated = {
                val destination = when (authViewModel.activeSalonState.value) {
                    is ActiveSalonUiState.SelectionRequired -> ReceptionDestinations.SALON_SELECTION
                    is ActiveSalonUiState.Error -> ReceptionDestinations.ACCESS_ERROR
                    else -> ReceptionDestinations.DASHBOARD
                }
                navController.navigate(destination) {
                    popUpTo(ReceptionDestinations.OTP_AUTH) { inclusive = true }
                }
            },
        )
    }

    composable(ReceptionDestinations.SALON_SELECTION) {
        ReceptionSalonSelectionScreen(
            viewModel = authViewModel,
            onSalonSelected = {
                navController.navigate(ReceptionDestinations.DASHBOARD) {
                    popUpTo(ReceptionDestinations.SALON_SELECTION) { inclusive = true }
                }
            },
        )
    }

    composable(ReceptionDestinations.ACCESS_ERROR) {
        ReceptionAccessErrorScreen(
            viewModel = authViewModel,
            onResolved = {
                val destination = when (authViewModel.activeSalonState.value) {
                    is ActiveSalonUiState.SelectionRequired -> ReceptionDestinations.SALON_SELECTION
                    else -> ReceptionDestinations.DASHBOARD
                }
                navController.navigate(destination) {
                    popUpTo(ReceptionDestinations.ACCESS_ERROR) { inclusive = true }
                }
            },
            onLogoutClick = {
                authViewModel.logout()
                navController.navigate(ReceptionDestinations.OTP_AUTH) {
                    popUpTo(ReceptionDestinations.ACCESS_ERROR) { inclusive = true }
                }
            },
        )
    }

    composable(ReceptionDestinations.DASHBOARD) {
        val appContext = LocalContext.current.applicationContext
        val salonId = requireActiveSalonId(authViewModel)
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
        val salonId = requireActiveSalonId(authViewModel)
        val customersViewModel: ReceptionCustomersViewModel = viewModel(
            factory = ReceptionCustomersViewModelFactory(appContext, salonId),
        )
        ReceptionCustomersListScreen(
            viewModel = customersViewModel,
            onBackClick = { navController.popBackStack() },
        )
    }

    composable(ReceptionDestinations.PROFILE) {
        val authState by authViewModel.authState.collectAsStateWithLifecycle()
        val identityContext by authViewModel.identityContext.collectAsStateWithLifecycle()
        val phoneNumber = when (val context = identityContext) {
            is UiState.Success -> context.data.phoneNumber
            else -> null
        }

        ReceptionProfileScreen(
            fullName = (authState as? ReceptionAuthState.Authenticated)?.fullName.orEmpty(),
            phoneNumber = phoneNumber,
            onBackClick = { navController.popBackStack() },
            onLogoutClick = {
                authViewModel.logout()
                navController.navigate(ReceptionDestinations.OTP_AUTH) {
                    popUpTo(ReceptionDestinations.DASHBOARD) { inclusive = true }
                }
            },
        )
    }

    // Booking wizard — nested graph so every screen inside shares ONE
    // ReceptionBookingViewModel instance, scoped to this graph's own
    // back-stack entry (mirrors ManagerNavGraph.kt's identical pattern).
    navigation(
        route = ReceptionDestinations.BOOKING_FLOW_GRAPH,
        startDestination = ReceptionDestinations.CREATE_APPOINTMENT,
    ) {
        composable(ReceptionDestinations.CREATE_APPOINTMENT) { backStackEntry ->
            val viewModel = receptionBookingViewModelFor(navController, backStackEntry, authViewModel)
            ReceptionBookingStartScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onStartClick = { navController.navigate(ReceptionDestinations.BOOKING_CUSTOMER) },
            )
        }

        composable(ReceptionDestinations.BOOKING_CUSTOMER) { backStackEntry ->
            val viewModel = receptionBookingViewModelFor(navController, backStackEntry, authViewModel)
            ReceptionBookingCustomerScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onCustomerSelected = { navController.navigate(ReceptionDestinations.BOOKING_SERVICE) },
            )
        }

        composable(ReceptionDestinations.BOOKING_SERVICE) { backStackEntry ->
            val viewModel = receptionBookingViewModelFor(navController, backStackEntry, authViewModel)
            ReceptionBookingServiceScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onServiceSelected = { navController.navigate(ReceptionDestinations.BOOKING_SPECIALIST) },
            )
        }

        composable(ReceptionDestinations.BOOKING_SPECIALIST) { backStackEntry ->
            val viewModel = receptionBookingViewModelFor(navController, backStackEntry, authViewModel)
            ReceptionBookingSpecialistScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onSpecialistSelected = { navController.navigate(ReceptionDestinations.BOOKING_DATETIME) },
            )
        }

        composable(ReceptionDestinations.BOOKING_DATETIME) { backStackEntry ->
            val viewModel = receptionBookingViewModelFor(navController, backStackEntry, authViewModel)
            ReceptionBookingDateTimeScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onContinueClick = { navController.navigate(ReceptionDestinations.BOOKING_REVIEW) },
            )
        }

        composable(ReceptionDestinations.BOOKING_REVIEW) { backStackEntry ->
            val viewModel = receptionBookingViewModelFor(navController, backStackEntry, authViewModel)
            ReceptionBookingReviewScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onConfirmed = { navController.navigate(ReceptionDestinations.BOOKING_SUCCESS) },
            )
        }

        composable(ReceptionDestinations.BOOKING_SUCCESS) {
            ReceptionBookingSuccessScreen(
                onDoneClick = {
                    // Pops the whole booking sub-graph at once — clears
                    // ReceptionBookingViewModel's state, matching
                    // ManagerNavGraph.kt's identical wiring.
                    navController.navigate(ReceptionDestinations.DASHBOARD) {
                        popUpTo(ReceptionDestinations.DASHBOARD) { inclusive = false }
                    }
                },
            )
        }
    }
}

/**
 * Safe by construction (see this file's own doc comment) — every call
 * site is only ever reached after [ActiveSalonUiState.Active] has already
 * resolved. Fails loudly rather than silently defaulting if that
 * invariant is ever violated, consistent with this codebase's established
 * "no unprovided palette/context renders mystery state" convention.
 */
@Composable
private fun requireActiveSalonId(authViewModel: ReceptionAuthViewModel): String {
    val activeSalonState by authViewModel.activeSalonState.collectAsStateWithLifecycle()
    val state = activeSalonState
    check(state is ActiveSalonUiState.Active) { "requireActiveSalonId called before ActiveSalonUiState.Active resolved" }
    return state.context.salonId
}

@Composable
private fun receptionBookingViewModelFor(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    authViewModel: ReceptionAuthViewModel,
): ReceptionBookingViewModel {
    val appContext = LocalContext.current.applicationContext
    val salonId = requireActiveSalonId(authViewModel)
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(ReceptionDestinations.BOOKING_FLOW_GRAPH)
    }
    return viewModel(
        viewModelStoreOwner = parentEntry,
        factory = ReceptionBookingViewModelFactory(appContext, salonId),
    )
}
