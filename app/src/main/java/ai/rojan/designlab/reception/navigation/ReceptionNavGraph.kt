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
import ai.rojan.designlab.reception.screens.splash.ReceptionSplashScreen
import ai.rojan.designlab.ui.navigation.navigateHomeAfterBooking
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
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
 * (constructed once, not per-screen). Every salon-scoped screen goes
 * through [WithActiveSalon], which supplies the resolved `salonId` when
 * the active salon is [ActiveSalonUiState.Active] and otherwise routes to
 * a recovery destination instead of crashing — see 5B7-1 below.
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
        WithActiveSalon(navController, authViewModel) { salonId ->
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
    }

    composable(ReceptionDestinations.CUSTOMERS) {
        WithActiveSalon(navController, authViewModel) { salonId ->
            val appContext = LocalContext.current.applicationContext
            val customersViewModel: ReceptionCustomersViewModel = viewModel(
                factory = ReceptionCustomersViewModelFactory(appContext, salonId),
            )
            ReceptionCustomersListScreen(
                viewModel = customersViewModel,
                onBackClick = { navController.popBackStack() },
            )
        }
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
            WithActiveSalon(navController, authViewModel) { salonId ->
                val viewModel = receptionBookingViewModelFor(navController, backStackEntry, salonId)
                ReceptionBookingStartScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onStartClick = { navController.navigate(ReceptionDestinations.BOOKING_CUSTOMER) },
                )
            }
        }

        composable(ReceptionDestinations.BOOKING_CUSTOMER) { backStackEntry ->
            WithActiveSalon(navController, authViewModel) { salonId ->
                val viewModel = receptionBookingViewModelFor(navController, backStackEntry, salonId)
                ReceptionBookingCustomerScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onCustomerSelected = { navController.navigate(ReceptionDestinations.BOOKING_SERVICE) },
                )
            }
        }

        composable(ReceptionDestinations.BOOKING_SERVICE) { backStackEntry ->
            WithActiveSalon(navController, authViewModel) { salonId ->
                val viewModel = receptionBookingViewModelFor(navController, backStackEntry, salonId)
                ReceptionBookingServiceScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onServiceSelected = { navController.navigate(ReceptionDestinations.BOOKING_SPECIALIST) },
                )
            }
        }

        composable(ReceptionDestinations.BOOKING_SPECIALIST) { backStackEntry ->
            WithActiveSalon(navController, authViewModel) { salonId ->
                val viewModel = receptionBookingViewModelFor(navController, backStackEntry, salonId)
                ReceptionBookingSpecialistScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onSpecialistSelected = { navController.navigate(ReceptionDestinations.BOOKING_DATETIME) },
                )
            }
        }

        composable(ReceptionDestinations.BOOKING_DATETIME) { backStackEntry ->
            WithActiveSalon(navController, authViewModel) { salonId ->
                val viewModel = receptionBookingViewModelFor(navController, backStackEntry, salonId)
                ReceptionBookingDateTimeScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onContinueClick = { navController.navigate(ReceptionDestinations.BOOKING_REVIEW) },
                )
            }
        }

        composable(ReceptionDestinations.BOOKING_REVIEW) { backStackEntry ->
            WithActiveSalon(navController, authViewModel) { salonId ->
                val viewModel = receptionBookingViewModelFor(navController, backStackEntry, salonId)
                ReceptionBookingReviewScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onConfirmed = { navController.navigate(ReceptionDestinations.BOOKING_SUCCESS) },
                )
            }
        }

        composable(ReceptionDestinations.BOOKING_SUCCESS) {
            ReceptionBookingSuccessScreen(
                onDoneClick = {
                    // 5B7-2: clear the whole back stack (including the finished
                    // booking sub-graph, which clears ReceptionBookingViewModel)
                    // and land on a single DASHBOARD. Matches ManagerNavGraph.kt.
                    navController.navigateHomeAfterBooking(ReceptionDestinations.DASHBOARD)
                },
            )
        }
    }
}

/**
 * 5B7-1 — the single choke point for every salon-scoped Reception
 * destination. [ReceptionRootGraph]'s splash gate only protects the
 * *initial* route: after Android process death, Jetpack Navigation
 * restores the saved back stack (a booking step / dashboard / customers)
 * regardless of `startDestination`, and the active salon may not have
 * re-resolved to [ActiveSalonUiState.Active] (a transient `/salon-access`
 * failure resolves it to [ActiveSalonUiState.Error]; a revoked session
 * leaves it [ActiveSalonUiState.Loading]). This previously reached a hard
 * `check(state is Active)` and crashed. Now:
 *
 * - [ActiveSalonUiState.Active] → render [content] with the resolved id
 *   (behaviour unchanged from before).
 * - otherwise → navigate once to the correct recovery destination,
 *   clearing the now-invalid back stack, and render a placeholder until
 *   the redirect lands. Recovery destinations do not route back through
 *   here, so there is no loop.
 */
@Composable
private fun WithActiveSalon(
    navController: NavController,
    authViewModel: ReceptionAuthViewModel,
    content: @Composable (salonId: String) -> Unit,
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val activeSalonState by authViewModel.activeSalonState.collectAsStateWithLifecycle()

    when (val gate = receptionSalonRecovery(authState, activeSalonState)) {
        is ReceptionSalonGate.Ready -> content(gate.salonId)
        is ReceptionSalonGate.Recover -> {
            LaunchedEffect(gate.destination) {
                navController.navigate(gate.destination) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
            ReceptionSplashScreen(onSplashFinished = {})
        }
        ReceptionSalonGate.Wait -> ReceptionSplashScreen(onSplashFinished = {})
    }
}

/**
 * Pure decision for [WithActiveSalon] — extracted so the recovery routing
 * (5B7-1) is unit-testable without a Compose/NavController harness.
 */
internal sealed interface ReceptionSalonGate {
    /** The active salon is resolved — render the salon-scoped screen. */
    data class Ready(val salonId: String) : ReceptionSalonGate

    /** Route to [destination] and drop the restored (now-invalid) back stack. */
    data class Recover(val destination: String) : ReceptionSalonGate

    /** Still resolving (authenticated, salon-access in flight) — show a placeholder and re-evaluate on the next emission. */
    data object Wait : ReceptionSalonGate
}

internal fun receptionSalonRecovery(
    authState: ReceptionAuthState,
    activeSalonState: ActiveSalonUiState,
): ReceptionSalonGate = when {
    activeSalonState is ActiveSalonUiState.Active -> ReceptionSalonGate.Ready(activeSalonState.context.salonId)
    // No valid session behind the restored screen → back to sign-in.
    authState !is ReceptionAuthState.Authenticated -> ReceptionSalonGate.Recover(ReceptionDestinations.OTP_AUTH)
    activeSalonState is ActiveSalonUiState.SelectionRequired -> ReceptionSalonGate.Recover(ReceptionDestinations.SALON_SELECTION)
    activeSalonState is ActiveSalonUiState.Error -> ReceptionSalonGate.Recover(ReceptionDestinations.ACCESS_ERROR)
    else -> ReceptionSalonGate.Wait // authenticated + Loading — resolution still in flight
}

@Composable
private fun receptionBookingViewModelFor(
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
        // 5B6-1: the extras Navigation-Compose restores SavedStateHandle through.
        extras = parentEntry.defaultViewModelCreationExtras,
    )
}
