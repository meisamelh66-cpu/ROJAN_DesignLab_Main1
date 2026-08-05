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
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.presentation.auth.AuthViewModelFactory
import ai.rojan.designlab.presentation.session.SessionRestoreState
import ai.rojan.designlab.presentation.session.SessionViewModel
import ai.rojan.designlab.presentation.session.SessionViewModelFactory
import ai.rojan.designlab.screens.auth.AuthScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * Self-contained nav graph for the isolated Manager App workspace.
 * Registers [ManagerDestinations.DASHBOARD], [ManagerDestinations.CALENDAR],
 * [ManagerDestinations.CUSTOMERS], [ManagerDestinations.CUSTOMER_PROFILE],
 * [ManagerDestinations.PROFILE], and the [ManagerDestinations.BOOKING_FLOW_GRAPH]
 * nested graph (services/staff/settings are still foundation folders
 * only, no screens yet). This is the real entry graph for the separately
 * installable ROJAN Manager app (`ManagerActivity`, `manager` product
 * flavor) — the shared `RojanNavGraph.kt`/Customer app are untouched
 * and unaffected.
 *
 * [ManagerDestinations.LOGIN] and the splash/session-restore gate are
 * *not* registered here (Manager Auth Flow Implementation) — they live in
 * [ManagerNavGraph], the top-level composable that owns this graph's
 * `NavHost` and decides its `startDestination`, since that decision has
 * to happen before this graph is even built.
 */
fun NavGraphBuilder.managerNavGraph(navController: NavController) {
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

/**
 * Manager App workspace — root composable, the Manager flavor's
 * counterpart to [ai.rojan.designlab.navigation.RojanNavGraph]. Owns the
 * `NavHost` [managerNavGraph] used to only be built directly inside
 * `ManagerActivity`, plus the authentication gate that was entirely
 * missing before (Manager Auth Flow Implementation — see
 * `ROJAN_Manager_Auth_Flow_Verification_Report_v1.md` for the
 * investigation that found this).
 *
 * Reuses the exact same building blocks [ai.rojan.designlab.navigation.RojanNavGraph]
 * already uses for the Customer flow — [SessionViewModel]/[SessionViewModelFactory]
 * (persisted-session restore, via the existing `TokenRepository`-backed
 * [AuthViewModel.restoreSession]), [AuthViewModel]/[AuthViewModelFactory]
 * (real `POST /api/v1/auth/login`/`/register`, via the existing
 * [ai.rojan.designlab.domain.repository.BackendAuthRepository]), and
 * [AuthScreen] itself, unmodified — no second auth system, no
 * Manager-specific login screen was built.
 *
 * [SessionRestoreState.Restored.personId] drives `startDestination`
 * exactly the way it drives Customer's own `CUSTOMER_HOME` vs. `EXPLORE`
 * choice in `RojanNavGraph.kt`: an optimistic, fast, local decision (a
 * persisted "remember me" person id existing at all) — not a wait for
 * [AuthViewModel.restoreSession]'s own real, async backend-token check to
 * finish. [ManagerDestinations.DASHBOARD] is deliberately left unguarded
 * (no live redirect if that later async check fails) for the same reason
 * Customer's `CUSTOMER_HOME` is: the only two ways to reach a personId at
 * all are (1) a `restoreSession` that already succeeded, or (2) a login
 * that already returned real tokens, so a *later* failure is a rare,
 * genuine edge case (e.g. server-side token revocation), not the normal
 * path — and every Manager screen already treats a failed/401 backend
 * call as an honest empty state, same as any other network failure this
 * codebase already handles, not a new gap this introduces. A simpler
 * splash timing than `RojanNavGraph`'s (which shows its own fixed-minimum-
 * duration splash independently of restore, then a *second*, separate
 * "still restoring" state): here [ManagerSplashScreen] is shown directly
 * for as long as [SessionRestoreState] is [SessionRestoreState.Loading]
 * (typically single-digit milliseconds - see [SessionViewModel]'s own doc
 * comment), with no extra fixed-minimum-duration polish layered on top -
 * a deliberate scope decision, not an oversight.
 */
@Composable
fun ManagerNavGraph() {
    val appContext = LocalContext.current.applicationContext
    val navController = rememberNavController()

    val sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModelFactory(appContext))
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(appContext))

    val restoreState by sessionViewModel.restoreState.collectAsStateWithLifecycle()

    when (val state = restoreState) {
        SessionRestoreState.Loading -> {
            ManagerSplashScreen()
        }

        is SessionRestoreState.Restored -> {
            // Hydrates authViewModel's in-memory session from the persisted personId (if any) -
            // same mechanism, same call, as ai.rojan.designlab.navigation.RojanNavGraph's own
            // LaunchedEffect(state) for the Customer flow. Internally calls
            // BackendAuthRepository.currentUser() (GET /api/v1/auth/me), which validates the
            // stored access token against the real backend (transparently refreshing it first via
            // TokenAuthenticator if expired) rather than trusting the persisted id blindly.
            LaunchedEffect(state) {
                state.personId?.let { authViewModel.restoreSession(it) }
            }

            // Captured once, at the first restore - see RojanNavGraph.kt's own doc comment on why
            // this must not be recomputed on a later restoreState emission (e.g. a subsequent
            // logout/login), which would otherwise reset navController's whole back stack.
            val startDestination = remember {
                if (state.personId != null) ManagerDestinations.DASHBOARD else ManagerDestinations.LOGIN
            }

            NavHost(navController = navController, startDestination = startDestination) {
                composable(ManagerDestinations.LOGIN) {
                    AuthScreen(
                        authViewModel = authViewModel,
                        onBackClick = {},
                        onExistingUserAuthenticated = {
                            navController.navigate(ManagerDestinations.DASHBOARD) {
                                popUpTo(ManagerDestinations.LOGIN) { inclusive = true }
                            }
                        },
                    )
                }

                managerNavGraph(navController)
            }
        }
    }
}
