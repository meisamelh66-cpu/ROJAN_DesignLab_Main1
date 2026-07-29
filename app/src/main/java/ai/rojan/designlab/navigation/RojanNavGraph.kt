package ai.rojan.designlab.navigation

import ai.rojan.designlab.R
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.components.PremiumLoadingBar
import ai.rojan.designlab.domain.booking.BookingIntent
import ai.rojan.designlab.domain.catalog.CatalogEngine
import ai.rojan.designlab.navigation.RojanDestinations.routeForBookingStep
import ai.rojan.designlab.presentation.booking.BookingViewModel
import ai.rojan.designlab.presentation.booking.BookingViewModelFactory
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel
import ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModelFactory
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.presentation.auth.AuthViewModelFactory
import ai.rojan.designlab.domain.identity.PersonRole
import ai.rojan.designlab.domain.identity.SessionState
import ai.rojan.designlab.presentation.session.SessionRestoreState
import ai.rojan.designlab.presentation.session.SessionViewModel
import ai.rojan.designlab.presentation.session.SessionViewModelFactory
import ai.rojan.designlab.screens.auth.AuthScreen
import ai.rojan.designlab.screens.auth.FirstTimeNameScreen
import ai.rojan.designlab.screens.booking.SalonListScreen
import ai.rojan.designlab.screens.booking.SpecialistSelectionScreen
import ai.rojan.designlab.screens.bookingflow.BookingConfirmationScreen
import ai.rojan.designlab.screens.bookingflow.BookingDateScreen
import ai.rojan.designlab.screens.bookingflow.BookingSuccessScreen
import ai.rojan.designlab.screens.bookingflow.BookingTimeScreen
import ai.rojan.designlab.screens.customer.CustomerDashboardScreen
import ai.rojan.designlab.screens.customer.CustomerHomeScreen
import ai.rojan.designlab.screens.customer.CustomerHomeTab
import ai.rojan.designlab.screens.dashboard.ManagerDashboardScreen
import ai.rojan.designlab.screens.dashboard.StylistDashboardScreen
import ai.rojan.designlab.screens.profile.AppointmentDetailsScreen
import ai.rojan.designlab.screens.profile.AppointmentsScreen
import ai.rojan.designlab.screens.profile.RescheduleAppointmentScreen
import ai.rojan.designlab.screens.profile.WaitlistScreen
import ai.rojan.designlab.screens.profile.BeautyTimelineScreen
import ai.rojan.designlab.screens.profile.CouponsScreen
import ai.rojan.designlab.screens.profile.FavoritesScreen
import ai.rojan.designlab.screens.profile.LoyaltyScreen
import ai.rojan.designlab.screens.profile.MembershipScreen
import ai.rojan.designlab.screens.profile.MyReviewsScreen
import ai.rojan.designlab.screens.profile.ProfileScreen
import ai.rojan.designlab.screens.profile.WalletScreen
import ai.rojan.designlab.screens.salon.SalonDetailsScreen
import ai.rojan.designlab.screens.search.SearchScreen
import ai.rojan.designlab.screens.service.ServiceDetailsScreen
import ai.rojan.designlab.screens.specialist.SpecialistProfileScreen
import ai.rojan.designlab.screens.splash.SplashScreen
import ai.rojan.designlab.ui.theme.RojanLuxuryCaption

import ai.rojan.designlab.ui.animation.RojanAnimations
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import ai.rojan.designlab.ui.text.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument


// Final Premium Polish, Phase 1: relocated (unchanged values) onto
// RojanAnimations.PageEnter/PageExit — the shared page-transition system
// now used to be the single source of truth for cross-screen navigation
// motion, instead of living only as private vals in this one file.
private val motionEnter = RojanAnimations.PageEnter
private val motionExit = RojanAnimations.PageExit


/**
 * Obtains the single [BookingViewModel] instance shared by every screen
 * inside the [RojanDestinations.BOOKING_FLOW_GRAPH] sub-graph, scoped to
 * that sub-graph's own back-stack entry rather than any individual
 * screen's. This is what makes the ViewModel survive navigation *within*
 * the flow while still being a fresh instance the next time the flow is
 * entered — see [BookingViewModel]'s own doc comment.
 */
@Composable
private fun bookingViewModelFor(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
): BookingViewModel {
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(RojanDestinations.BOOKING_FLOW_GRAPH)
    }
    return viewModel<BookingViewModel>(
        viewModelStoreOwner = parentEntry,
        factory = BookingViewModelFactory,
        extras = parentEntry.defaultViewModelCreationExtras,
    )
}

/**
 * UX Refactor Phase 1: tells [RojanDestinations.AUTH]/[RojanDestinations.FIRST_TIME_NAME]
 * whether they were reached as the new mid-booking-flow "log in only when
 * booking" gate (from [RojanDestinations.BOOKING_TIME]) versus the older
 * pre-booking entry point (from [RojanDestinations.CUSTOMER_HOME]'s hero
 * card). [RojanDestinations.BOOKING_FLOW_GRAPH]'s own back-stack entry is
 * only present once that nested graph has actually been entered, and
 * navigating from BOOKING_TIME to AUTH doesn't pop it — so its presence
 * is a reliable, already-existing signal, not new state to track.
 */
private fun NavController.hasBookingFlowInProgress(): Boolean =
    runCatching { getBackStackEntry(RojanDestinations.BOOKING_FLOW_GRAPH) }.isSuccess

/**
 * UX Refactor Phase 3: the same back-stack-presence signal as
 * [hasBookingFlowInProgress], for the business-login entry point instead.
 * Deliberately checks for [RojanDestinations.WELCOME]'s presence anywhere
 * in the back stack, not just [NavController.previousBackStackEntry] —
 * [RojanDestinations.FIRST_TIME_NAME]'s immediate previous entry is
 * always [RojanDestinations.AUTH] regardless of which flow led there, so
 * an immediate-parent check would silently misdetect that screen.
 */
private fun NavController.hasBusinessLoginInProgress(): Boolean =
    runCatching { getBackStackEntry(RojanDestinations.WELCOME) }.isSuccess

// Customer Journey Audit (Booking Success P0): CustomerEcosystemViewModel
// used to be obtained per-nested-graph (a separate instance for
// CUSTOMER_HOME, PROFILE_GRAPH, and BookingTimeScreen each) - meaning a
// booking completed via Search -> ... -> Confirmation had no reliable,
// always-present instance to record an appointment into (PROFILE_GRAPH's
// back-stack entry doesn't exist until the user has actually visited
// Profile/Appointments/Favorites at least once in the session, so
// obtaining it there would crash for the common "Home -> Search"
// direct path). Hoisted to session scope instead - obtained once below,
// alongside sessionViewModel/authViewModel, and threaded through to
// every consumer that previously created its own local instance.

/**
 * Production Readiness Audit (V1.0 Module 6): real navigation guard for
 * Customer-only screens (Appointments, Waitlist, Reschedule, Favorites,
 * Appointment Details).
 *
 * UX Refactor Phase 2: now checks real authenticated identity
 * ([AuthViewModel.sessionState] is [SessionState.LoggedIn]) instead of
 * the OLD, coarse `Role` flag this guard used through Phase 1 — see this
 * phase's plan doc for why that was a deliberate, disclosed interim
 * choice rather than the correct long-term check. Reading via
 * [collectAsStateWithLifecycle] (reactive) rather than a one-off `.value`
 * snapshot, for consistency with every other session-state read in this
 * file.
 *
 * No `Loading` branch is needed here (unlike the old `Role`-based
 * version): this guard is only ever composed for a screen inside
 * `NavHost`, which itself only exists once [SessionRestoreState] has
 * already resolved to `Restored` — by which point `authViewModel` has
 * already been synchronously hydrated (see the `LaunchedEffect(state)`
 * in [RojanNavGraph] that calls [AuthViewModel.restoreSession]), well
 * before a user could navigate to a guarded route.
 */
@Composable
private fun CustomerAccessGuard(
    authViewModel: AuthViewModel,
    onAccessDenied: () -> Unit,
    content: @Composable () -> Unit,
) {
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    if (sessionState is SessionState.LoggedIn) {
        content()
    } else {
        LaunchedEffect(Unit) { onAccessDenied() }
    }
}

/**
 * UX Refactor Phase 3: defense-in-depth guard for the staff dashboards —
 * mirrors [CustomerAccessGuard]'s exact shape. [MANAGER_DASHBOARD]/
 * [STYLIST_DASHBOARD] are now a real access boundary (real [PersonRole]
 * assignments, not a tap-a-card destination), so they get the same kind
 * of guard the customer-only screens already have, not just correct
 * routing from the business-login flow.
 */
@Composable
private fun StaffAccessGuard(
    authViewModel: AuthViewModel,
    allowedRoles: Set<PersonRole>,
    onAccessDenied: () -> Unit,
    content: @Composable () -> Unit,
) {
    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
    val hasAccess = sessionState is SessionState.LoggedIn &&
        authViewModel.currentPersonRoles().any { it in allowedRoles }
    if (hasAccess) {
        content()
    } else {
        LaunchedEffect(Unit) { onAccessDenied() }
    }
}


@Composable
fun RojanNavGraph() {

    val appContext = LocalContext.current.applicationContext

    val navController = rememberNavController()


    var showSplash by remember {
        mutableStateOf(true)
    }


    val sessionViewModel: SessionViewModel = viewModel(
        factory = SessionViewModelFactory(appContext)
    )

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(appContext)
    )

    val customerEcosystemViewModel: CustomerEcosystemViewModel = viewModel(
        factory = CustomerEcosystemViewModelFactory()
    )


    val restoreState by sessionViewModel.restoreState
        .collectAsStateWithLifecycle()



    if (showSplash) {

        SplashScreen(
            onSplashFinished = {
                showSplash = false
            }
        )

        return
    }



    when (val state = restoreState) {


        SessionRestoreState.Loading -> {

            RestoringSessionContent()

        }



        is SessionRestoreState.Restored -> {

            // UX Refactor Phase 2: hydrates authViewModel's in-memory
            // session from the persisted personId (if any) before any
            // screen below can read it — synchronous, no suspend point,
            // so this completes before a user could navigate anywhere.
            // Replaces Phase 1's one-way bridge (which wrote Role.CUSTOMER
            // on OTP success so cold starts could fake a "logged in"
            // signal); this restores the real identity instead.
            LaunchedEffect(state) {
                state.personId?.let { authViewModel.restoreSession(it) }
            }

            // Bug fix: startDestination must be captured once, at the first
            // cold-start restore, not recomputed on every recomposition of
            // `state`. `state` (SessionRestoreState.Restored) changes again
            // mid-session whenever SessionViewModel's observePersonId() flow
            // re-emits — e.g. every OTP login, including one that happens
            // mid-booking-flow via the AUTH screen. Recomputing this value
            // on that later emission made NavHost rebuild its graph with a
            // new start destination, which resets the NavController back to
            // it — silently discarding the live booking-flow back stack
            // (up to and including a just-reached BOOKING_CONFIRMATION) and
            // landing on CUSTOMER_HOME instead. `remember` with no keys
            // freezes this to the value from the initial composition only.
            // UX Flow correction: an unauthenticated (first-time or logged-out)
            // customer now lands on EXPLORE (the marketplace: search,
            // services, salons, specialists) instead of MEMBER_SALONS_LIST —
            // login only happens once they act on a booking intent, per the
            // existing "login only when booking" gate on the booking flow.
            // Authenticated customers still land on CUSTOMER_HOME (the
            // Dashboard), unchanged.
            val startDestination = remember {
                RojanDestinations.routeForPersonRoles(state.personRoles)
                    ?: if (state.personId != null) {
                        RojanDestinations.CUSTOMER_HOME
                    } else {
                        RojanDestinations.EXPLORE
                    }
            }

            NavHost(
                navController = navController,
                // Staff routing (like customer routing) is identity-based —
                // resolved entirely from state.personRoles, looked up by
                // SessionViewModel at restore time. The older, DataStore-
                // persisted Role enum this used to read (UX Refactor Phase
                // 1/2) was retired in Phase 4 once nothing wrote or read it
                // any more.
                startDestination = startDestination
            ) {



                composable(
                    route = RojanDestinations.MEMBER_SALONS_LIST,
                    enterTransition = { motionEnter },
                    exitTransition = { motionExit },
                ) {
                    SalonListScreen(
                        selectedServiceIds = emptyList(),
                        showBackButton = false,
                        onBackClick = {},
                        onSalonSelected = { salonId ->
                            navController.navigate(RojanDestinations.salonDetails(salonId))
                        },
                        // Secondary entry point for Manager/Specialist roles —
                        // WELCOME is no longer the default landing screen.
                        onBusinessLoginClick = { navController.navigate(RojanDestinations.WELCOME) },
                    )
                }




                composable(
                    route = RojanDestinations.WELCOME,
                    enterTransition = {
                        motionEnter
                    },
                    exitTransition = {
                        motionExit
                    }
                ) {

                    WelcomeRoute(
                        navController = navController
                    )

                }




                composable(
                    route = RojanDestinations.AUTH,
                    enterTransition = { motionEnter },
                    exitTransition = { motionExit },
                ) { backStackEntry ->
                    // UX Refactor Phase 1: must be read here, at composition
                    // time (bookingViewModelFor is @Composable) — not inside
                    // the click callbacks below, which run outside composition.
                    val bookingFlowInProgress = navController.hasBookingFlowInProgress()
                    val inProgressBookingViewModel = if (bookingFlowInProgress) {
                        bookingViewModelFor(navController, backStackEntry)
                    } else {
                        null
                    }
                    AuthScreen(
                        authViewModel = authViewModel,
                        onBackClick = { navController.popBackStack() },
                        onExistingUserAuthenticated = {
                            // UX Refactor Phase 2: personId persistence now
                            // happens inside AuthViewModel.submitOtp itself —
                            // no bridge call needed here any more.
                            // "Login/OTP only when booking" — resumes exactly
                            // where the booking flow left off.
                            //
                            // UX Refactor Phase 3: business-login branch —
                            // real PersonRole (not which card was tapped)
                            // decides Manager vs Specialist vs denial. Denial
                            // stays on this same screen: denyAccessAndLogout
                            // reverts sessionState to LoggedOut, which
                            // AuthScreen already reflects by re-enabling the
                            // phone field and showing the error message — no
                            // navigation needed for that case.
                            when {
                                inProgressBookingViewModel != null -> {
                                    val targetStep = inProgressBookingViewModel.nextStep()
                                    val targetRoute = routeForBookingStep(targetStep, inProgressBookingViewModel.state.salonId)
                                    // Booking Flow Fix (P0): popUpTo(AUTH) here — a
                                    // route OUTSIDE BOOKING_FLOW_GRAPH — combined with
                                    // navigating straight to a route INSIDE that nested
                                    // graph, made Navigation-Compose spin up a *second*,
                                    // empty instance of the graph instead of reusing the
                                    // one already on the back stack: bookingViewModelFor's
                                    // navController.getBackStackEntry(BOOKING_FLOW_GRAPH)
                                    // returned a different NavBackStackEntry once
                                    // Confirmation composed, so it got a fresh, empty
                                    // BookingViewModel — reproduced and confirmed via
                                    // device testing (logged parentEntry identity hashes
                                    // differed). BOOKING_TIME is the confirmed immediate
                                    // predecessor whenever this branch is reached (per
                                    // hasBookingFlowInProgress's own doc comment) and is
                                    // itself already inside the graph, so popping up to
                                    // it (not AUTH) never crosses the graph boundary and
                                    // never triggers the re-creation.
                                    navController.navigate(targetRoute) {
                                        popUpTo(RojanDestinations.BOOKING_TIME) { inclusive = false }
                                    }
                                }
                                navController.hasBusinessLoginInProgress() -> {
                                    val staffRoute = RojanDestinations.routeForPersonRoles(authViewModel.currentPersonRoles())
                                    if (staffRoute != null) {
                                        navController.navigate(staffRoute) {
                                            popUpTo(RojanDestinations.WELCOME) { inclusive = true }
                                        }
                                    } else {
                                        authViewModel.denyAccessAndLogout("این شماره دسترسی کسب‌وکار ندارد")
                                    }
                                }
                                else -> {
                                    // Defensive only — no current call site
                                    // reaches AUTH any other way.
                                    navController.navigate(RojanDestinations.MEMBER_SALONS_LIST) {
                                        popUpTo(RojanDestinations.AUTH) { inclusive = true }
                                    }
                                }
                            }
                        },
                        onFirstTimeUser = {
                            navController.navigate(RojanDestinations.FIRST_TIME_NAME)
                        },
                    )
                }

                composable(
                    route = RojanDestinations.FIRST_TIME_NAME,
                    enterTransition = { motionEnter },
                    exitTransition = { motionExit },
                ) { backStackEntry ->
                    // Bug fix: DemoSessionProvider's SessionState.AwaitingFirstName
                    // is in-memory only, not persisted — a process death while on
                    // this screen (e.g. a customer switching away to read their
                    // OTP SMS) restores this *route* via Navigation's own
                    // back-stack persistence, but the fresh AuthViewModel/
                    // SessionProvider restarts at LoggedOut. Submitting the name
                    // then crashed with IllegalStateException from
                    // DemoSessionProvider.createFirstTimeUser (it requires
                    // AwaitingFirstName). Guard this the same way
                    // CustomerAccessGuard/StaffAccessGuard above guard their
                    // screens: if the real session state doesn't match what this
                    // screen requires, bounce back to AUTH (phone entry) — a safe,
                    // recoverable screen — instead of rendering a screen whose
                    // only action would crash.
                    val sessionState by authViewModel.sessionState.collectAsStateWithLifecycle()
                    if (sessionState !is SessionState.AwaitingFirstName) {
                        LaunchedEffect(Unit) {
                            navController.navigate(RojanDestinations.AUTH) {
                                popUpTo(RojanDestinations.FIRST_TIME_NAME) { inclusive = true }
                            }
                        }
                        return@composable
                    }
                    val bookingFlowInProgress = navController.hasBookingFlowInProgress()
                    val inProgressBookingViewModel = if (bookingFlowInProgress) {
                        bookingViewModelFor(navController, backStackEntry)
                    } else {
                        null
                    }
                    FirstTimeNameScreen(
                        authViewModel = authViewModel,
                        onBackClick = { navController.popBackStack() },
                        onNameSubmitted = {
                            // UX Refactor Phase 2: personId persistence now
                            // happens inside AuthViewModel.submitFirstName
                            // itself — no bridge call needed here any more.
                            // Same three-way branch as AUTH above. Unlike
                            // AuthScreen, this screen has no phone field to
                            // fall back to on denial — a first-time signup
                            // through the business-login entry point is
                            // always denied (registerPerson only ever grants
                            // PersonRole.CUSTOMER), so send the user back to
                            // AUTH to see the denial on the phone-entry screen.
                            when {
                                inProgressBookingViewModel != null -> {
                                    val targetStep = inProgressBookingViewModel.nextStep()
                                    val targetRoute = routeForBookingStep(targetStep, inProgressBookingViewModel.state.salonId)
                                    // Booking Flow Fix (P0) — see identical comment on
                                    // AUTH's own onExistingUserAuthenticated above: popping
                                    // up to BOOKING_TIME (inside the graph), not AUTH
                                    // (outside it), is what prevents Navigation-Compose
                                    // from creating a second, empty BOOKING_FLOW_GRAPH
                                    // instance for Confirmation to read from.
                                    navController.navigate(targetRoute) {
                                        popUpTo(RojanDestinations.BOOKING_TIME) { inclusive = false }
                                    }
                                }
                                navController.hasBusinessLoginInProgress() -> {
                                    val staffRoute = RojanDestinations.routeForPersonRoles(authViewModel.currentPersonRoles())
                                    if (staffRoute != null) {
                                        navController.navigate(staffRoute) {
                                            popUpTo(RojanDestinations.WELCOME) { inclusive = true }
                                        }
                                    } else {
                                        authViewModel.denyAccessAndLogout("این شماره دسترسی کسب‌وکار ندارد")
                                        navController.navigate(RojanDestinations.AUTH) {
                                            popUpTo(RojanDestinations.AUTH) { inclusive = true }
                                        }
                                    }
                                }
                                else -> {
                                    navController.navigate(RojanDestinations.MEMBER_SALONS_LIST) {
                                        popUpTo(RojanDestinations.AUTH) { inclusive = true }
                                    }
                                }
                            }
                        },
                    )
                }

                composable(
                    route = RojanDestinations.SALON_LIST,
                    arguments = listOf(navArgument("selectedServiceIds") { type = NavType.StringType }),
                    enterTransition = { motionEnter },
                    exitTransition = { motionExit },
                ) { backStackEntry ->
                    val selectedServiceIds = backStackEntry.arguments?.getString("selectedServiceIds")
                        ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                    SalonListScreen(
                        selectedServiceIds = selectedServiceIds,
                        onBackClick = { navController.popBackStack() },
                        onSalonSelected = { salonId ->
                            navController.navigate(RojanDestinations.salonDetails(salonId))
                        },
                    )
                }




                // ── Journey 1 booking flow: a nested graph so every screen in it
                // shares ONE BookingViewModel instance, scoped to this graph's own
                // back-stack entry. Jetpack Navigation clears that ViewModel's
                // store automatically once this whole graph is popped — see
                // BookingViewModel's own doc comment for why that's the point.
                navigation(
                    route = RojanDestinations.BOOKING_FLOW_GRAPH,
                    startDestination = RojanDestinations.SEARCH,
                ) {

                    composable(
                        route = RojanDestinations.SEARCH,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        SearchScreen(
                            onBackClick = { navController.popBackStack() },
                            onSalonClick = { salonId ->
                                navController.navigate(RojanDestinations.salonDetails(salonId))
                            },
                        )
                    }




                    composable(
                        route = RojanDestinations.SALON_DETAILS,
                        arguments = listOf(
                            navArgument("salonId") { type = NavType.StringType },
                            navArgument("selectedServiceIds") { type = NavType.StringType; nullable = true },
                        ),
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val bookingViewModel = bookingViewModelFor(navController, backStackEntry)
                        val salonId = backStackEntry.arguments?.getString("salonId") ?: ""
                        val selectedServiceIds = backStackEntry.arguments?.getString("selectedServiceIds")
                            ?.split(",")?.filter { it.isNotBlank() }
                        SalonDetailsScreen(
                            salonId = salonId,
                            ecosystemViewModel = customerEcosystemViewModel,
                            selectedServiceIds = selectedServiceIds,
                            onBackClick = { navController.popBackStack() },
                            onSpecialistClick = { specialistId ->
                                navController.navigate(RojanDestinations.specialistProfile(specialistId))
                            },
                            onServiceClick = { serviceId ->
                                bookingViewModel.onSalonSelected(salonId)
                                bookingViewModel.onIntentDetected(BookingIntent.SALON)
                                navController.navigate(RojanDestinations.serviceDetails(serviceId))
                            },
                            onContinueBooking = if (selectedServiceIds != null) { autoSpecialistId ->
                                bookingViewModel.onSalonSelected(salonId)
                                if (autoSpecialistId != null) {
                                    bookingViewModel.onSpecialistSelected(autoSpecialistId)
                                    navController.navigate(RojanDestinations.BOOKING_DATE)
                                } else {
                                    navController.navigate(RojanDestinations.specialistSelection(salonId))
                                }
                            } else null,
                        )
                    }

                    composable(
                        route = RojanDestinations.SPECIALIST_SELECTION,
                        arguments = listOf(navArgument("salonId") { type = NavType.StringType }),
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val bookingViewModel = bookingViewModelFor(navController, backStackEntry)
                        val salonId = backStackEntry.arguments?.getString("salonId") ?: ""
                        val catalogEngineForDuration = CatalogEngine()
                        val durationMinutes = bookingViewModel.state.serviceId
                            ?.let { catalogEngineForDuration.findServiceById(it)?.durationMinutes }
                            ?: 30
                        SpecialistSelectionScreen(
                            salonId = salonId,
                            durationMinutes = durationMinutes,
                            onBackClick = { navController.popBackStack() },
                            onSpecialistSelected = { specialistId ->
                                bookingViewModel.onSpecialistSelected(specialistId)
                                navController.navigate(RojanDestinations.BOOKING_DATE)
                            },
                        )
                    }




                    composable(
                        route = RojanDestinations.SPECIALIST_PROFILE,
                        arguments = listOf(navArgument("specialistId") { type = NavType.StringType }),
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val bookingViewModel = bookingViewModelFor(navController, backStackEntry)
                        val specialistId = backStackEntry.arguments?.getString("specialistId") ?: ""
                        val catalogEngineForSpecialist = CatalogEngine()
                        val salonIdForSpecialist = catalogEngineForSpecialist.findSpecialistById(specialistId)?.salonId
                        SpecialistProfileScreen(
                            specialistId = specialistId,
                            onBackClick = { navController.popBackStack() },
                            onServiceClick = { serviceId ->
                                // P0 fix: this path (Salon Details → a specific
                                // specialist → Service Details) previously never
                                // recorded salonId, leaving BookingState.salonId
                                // null through Confirmation/Success. The specialist
                                // already knows their own salon, so record it from
                                // there rather than requiring this screen's caller
                                // to have passed one in.
                                if (salonIdForSpecialist != null) {
                                    bookingViewModel.onSalonSelected(salonIdForSpecialist)
                                }
                                bookingViewModel.onSpecialistSelected(specialistId)
                                bookingViewModel.onIntentDetected(BookingIntent.SPECIALIST)
                                navController.navigate(RojanDestinations.serviceDetails(serviceId))
                            },
                        )
                    }




                    composable(
                        route = RojanDestinations.SERVICE_DETAILS,
                        arguments = listOf(navArgument("serviceId") { type = NavType.StringType }),
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val bookingViewModel = bookingViewModelFor(navController, backStackEntry)
                        val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
                        val catalogEngineForSpecialistCheck = CatalogEngine()
                        ServiceDetailsScreen(
                            serviceId = serviceId,
                            onBackClick = { navController.popBackStack() },
                            onBookClick = {
                                bookingViewModel.onServiceSelected(serviceId)
                                if (bookingViewModel.state.intent == BookingIntent.UNKNOWN) {
                                    bookingViewModel.onIntentDetected(BookingIntent.SERVICE)
                                }
                                // Customer Journey Audit Phase A (P0-1) fix:
                                // auto-select the specialist when the salon
                                // only has one — mirrors SalonDetailsScreen's
                                // existing onContinueBooking behavior — so
                                // BookingStepResolver only asks the customer
                                // when there's a genuine choice to make.
                                if (bookingViewModel.state.specialistId == null) {
                                    val specialists = bookingViewModel.state.salonId
                                        ?.let { catalogEngineForSpecialistCheck.specialistsForSalon(it) }
                                        ?: emptyList()
                                    if (specialists.size == 1) {
                                        bookingViewModel.onSpecialistSelected(specialists.first().id)
                                    }
                                }
                                navController.navigate(
                                    routeForBookingStep(bookingViewModel.nextStep(), bookingViewModel.state.salonId)
                                )
                            },
                        )
                    }




                    composable(
                        route = RojanDestinations.BOOKING_DATE,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val bookingViewModel = bookingViewModelFor(navController, backStackEntry)
                        BookingDateScreen(
                            bookingViewModel = bookingViewModel,
                            onBackClick = { navController.popBackStack() },
                            onDateSelected = { dateKey ->
                                bookingViewModel.onDateSelected(dateKey)
                                navController.navigate(
                                    routeForBookingStep(bookingViewModel.nextStep(), bookingViewModel.state.salonId)
                                )
                            },
                        )
                    }




                    composable(
                        route = RojanDestinations.BOOKING_TIME,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val bookingViewModel = bookingViewModelFor(navController, backStackEntry)
                        BookingTimeScreen(
                            dateKey = bookingViewModel.state.selectedDateKey ?: "today",
                            bookingViewModel = bookingViewModel,
                            ecosystemViewModel = customerEcosystemViewModel,
                            onBackClick = { navController.popBackStack() },
                            onTimeSelected = { time ->
                                bookingViewModel.onTimeSelected(time)
                                // UX Refactor Phase 1: "Login/OTP only when
                                // booking" — an already-authenticated (this
                                // session) customer proceeds straight to
                                // Confirmation; everyone else is routed
                                // through AUTH first. AUTH's own success
                                // handler resumes at routeForBookingStep(
                                // bookingViewModel.nextStep()) — the same
                                // call used here — so both paths converge
                                // on the same destination with this exact
                                // BookingViewModel instance's state intact.
                                if (authViewModel.sessionState.value is SessionState.LoggedIn) {
                                    navController.navigate(
                                        routeForBookingStep(bookingViewModel.nextStep(), bookingViewModel.state.salonId)
                                    )
                                } else {
                                    navController.navigate(RojanDestinations.AUTH)
                                }
                            },
                        )
                    }




                    composable(
                        route = RojanDestinations.BOOKING_CONFIRMATION,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val bookingViewModel = bookingViewModelFor(navController, backStackEntry)
                        BookingConfirmationScreen(
                            bookingViewModel = bookingViewModel,
                            onBackClick = { navController.popBackStack() },
                            // Editable summary rows: reuse the exact same picker
                            // screens/routes the forward flow already uses for
                            // each field, rather than introducing new ones. Once
                            // the customer makes a new selection there, that
                            // screen's own onXSelected callback already routes
                            // forward via routeForBookingStep(bookingViewModel.nextStep(), ...)
                            // — since every other field is already filled, that
                            // resolves straight back to CONFIRMATION with the
                            // one edited field updated.
                            onEditSalon = {
                                bookingViewModel.state.salonId?.let {
                                    navController.navigate(RojanDestinations.salonDetails(it))
                                }
                            },
                            onEditSpecialist = {
                                bookingViewModel.state.salonId?.let {
                                    navController.navigate(RojanDestinations.specialistSelection(it))
                                }
                            },
                            onEditService = {
                                val salonId = bookingViewModel.state.salonId
                                navController.navigate(
                                    if (salonId != null) RojanDestinations.salonDetails(salonId) else RojanDestinations.SEARCH
                                )
                            },
                            onEditDate = { navController.navigate(RojanDestinations.BOOKING_DATE) },
                            onEditTime = { navController.navigate(RojanDestinations.BOOKING_TIME) },
                            onConfirmClick = {
                                // Customer Journey Audit (Booking Success P0): record the
                                // completed booking as a real appointment before leaving
                                // this graph - BookingViewModel's state is destroyed once
                                // the sub-graph pops on Success's "Done", so this is the
                                // last point it's readable. Mirrors exactly what this same
                                // screen already displays (same fallback strings), so the
                                // recorded appointment matches what the user confirmed.
                                val confirmedState = bookingViewModel.state
                                val catalogEngineForConfirm = CatalogEngine()
                                val service = confirmedState.serviceId?.let { catalogEngineForConfirm.findServiceById(it) }
                                val dateLabel = confirmedState.selectedDateKey?.let { catalogEngineForConfirm.dateLabelFor(it) }
                                val time = confirmedState.selectedTime
                                if (service != null && confirmedState.selectedDateKey != null && dateLabel != null && time != null) {
                                    val salon = confirmedState.salonId?.let { catalogEngineForConfirm.findSalonById(it) }
                                    val specialist = confirmedState.specialistId?.let { catalogEngineForConfirm.findSpecialistById(it) }
                                    customerEcosystemViewModel.bookAppointment(
                                        salonName = salon?.name ?: "—",
                                        serviceName = service.name,
                                        specialistName = specialist?.name ?: "انتخاب خودکار",
                                        serviceId = service.id,
                                        specialistId = specialist?.id,
                                        dateKey = confirmedState.selectedDateKey,
                                        dateLabel = dateLabel,
                                        time = time,
                                        price = service.discountPrice ?: service.price,
                                        salonId = confirmedState.salonId,
                                        paymentMethod = confirmedState.paymentMethod,
                                    )
                                }
                                navController.navigate(RojanDestinations.BOOKING_SUCCESS)
                            },
                        )
                    }




                    composable(
                        route = RojanDestinations.BOOKING_SUCCESS,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) {
                        BookingSuccessScreen(
                            onDoneClick = {
                                // Pops the whole booking sub-graph at once - this is what
                                // clears BookingViewModel's state (see its own doc comment),
                                // not a manual reset call.
                                //
                                // UX Refactor Phase 1: always CUSTOMER_HOME now,
                                // not conditional on how this user started out —
                                // Booking Confirmation is only reachable once
                                // authenticated (the Login/OTP gate on
                                // BOOKING_TIME), so by the time anyone reaches
                                // Success they're already a customer, whether
                                // they just completed their first login or were
                                // a returning registered customer all along.
                                navController.navigate(RojanDestinations.CUSTOMER_HOME) {
                                    popUpTo(RojanDestinations.CUSTOMER_HOME) { inclusive = false }
                                }
                            },
                        )
                    }

                }




                composable(
                    route = RojanDestinations.CUSTOMER_HOME,
                    enterTransition = {
                        motionEnter
                    },
                    exitTransition = {
                        motionExit
                    }
                ) {
                    // UX Correction (Explore Repositioning): CUSTOMER_HOME is now
                    // the Dashboard, not the marketplace-heavy screen — see
                    // CustomerDashboardScreen's own doc comment.
                    CustomerDashboardScreen(
                        ecosystemViewModel = customerEcosystemViewModel,
                        authViewModel = authViewModel,
                        onProfileClick = { navController.navigate(RojanDestinations.PROFILE) },
                        onBookAppointmentClick = { navController.navigate(RojanDestinations.MEMBER_SALONS_LIST) },
                        onBookingsClick = { navController.navigate(RojanDestinations.APPOINTMENTS) },
                        onFavoritesClick = { navController.navigate(RojanDestinations.FAVORITES) },
                        onExploreClick = { navController.navigate(RojanDestinations.EXPLORE) },
                        onSalonClick = { salonId ->
                            navController.navigate(RojanDestinations.salonDetails(salonId))
                        },
                    )

                }

                composable(
                    route = RojanDestinations.EXPLORE,
                    enterTransition = {
                        motionEnter
                    },
                    exitTransition = {
                        motionExit
                    }
                ) {
                    // UX Correction (Explore Repositioning): the former
                    // CUSTOMER_HOME screen, unchanged, reached from the
                    // Dashboard instead of being the first thing shown.
                    //
                    // Routing-identity fix: this route serves two contexts —
                    // a brand-new/unauthenticated user's actual Landing
                    // screen (no prior back-stack entry, since EXPLORE is
                    // their NavHost startDestination) versus the Dashboard's
                    // "جستجو" tab destination (reached via an explicit
                    // navigate() call, so a previous entry exists). The
                    // bottom bar's active tab now reflects which one this
                    // is, instead of always reading "جستجو" even when this
                    // screen IS the Landing screen.
                    val isLandingEntry = navController.previousBackStackEntry == null
                    CustomerHomeScreen(
                        ecosystemViewModel = customerEcosystemViewModel,
                        authViewModel = authViewModel,
                        bottomBarActiveTab = if (isLandingEntry) CustomerHomeTab.HOME else CustomerHomeTab.SEARCH,
                        onProfileClick = { navController.navigate(RojanDestinations.PROFILE) },
                        // UX Refactor Phase 1: was AUTH directly — the
                        // category-first flow that used to follow login no
                        // longer exists, and "login only when booking" means
                        // this CTA should start with salon browsing, not a
                        // phone number prompt. Repointed to the same browse
                        // list a brand-new customer starts on.
                        onBookAppointmentClick = { navController.navigate(RojanDestinations.MEMBER_SALONS_LIST) },
                        onBookingsClick = { navController.navigate(RojanDestinations.APPOINTMENTS) },
                        onFavoritesClick = { navController.navigate(RojanDestinations.FAVORITES) },
                        onSearchClick = { navController.navigate(RojanDestinations.SEARCH) },
                        onHomeClick = {
                            navController.navigate(RojanDestinations.CUSTOMER_HOME) {
                                popUpTo(RojanDestinations.CUSTOMER_HOME) { inclusive = false }
                            }
                        },
                        onSalonClick = { salonId ->
                            navController.navigate(RojanDestinations.salonDetails(salonId))
                        },
                    )
                }




                navigation(
                    route = RojanDestinations.PROFILE_GRAPH,
                    startDestination = RojanDestinations.PROFILE,
                ) {

                    composable(
                        route = RojanDestinations.PROFILE,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val ecosystemViewModel = customerEcosystemViewModel
                        ProfileScreen(
                            ecosystemViewModel = ecosystemViewModel,
                            authViewModel = authViewModel,
                            onBackClick = { navController.popBackStack() },
                            onAppointmentsClick = { navController.navigate(RojanDestinations.APPOINTMENTS) },
                            onFavoritesClick = { navController.navigate(RojanDestinations.FAVORITES) },
                            onWalletClick = { navController.navigate(RojanDestinations.WALLET) },
                            onCouponsClick = { navController.navigate(RojanDestinations.COUPONS) },
                            onMembershipClick = { navController.navigate(RojanDestinations.MEMBERSHIP) },
                            onLoyaltyClick = { navController.navigate(RojanDestinations.LOYALTY) },
                            onReviewsClick = { navController.navigate(RojanDestinations.MY_REVIEWS) },
                            onBeautyTimelineClick = { navController.navigate(RojanDestinations.BEAUTY_TIMELINE) },
                        )
                    }




                    composable(
                        route = RojanDestinations.APPOINTMENTS,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        CustomerAccessGuard(
                            authViewModel = authViewModel,
                            onAccessDenied = { navController.popBackStack() },
                        ) {
                            val ecosystemViewModel = customerEcosystemViewModel
                            AppointmentsScreen(
                                ecosystemViewModel = ecosystemViewModel,
                                onBackClick = { navController.popBackStack() },
                                onAppointmentClick = { appointmentId ->
                                    navController.navigate(RojanDestinations.appointmentDetails(appointmentId))
                                },
                                onRescheduleClick = { appointmentId ->
                                    navController.navigate(RojanDestinations.rescheduleAppointment(appointmentId))
                                },
                                onWaitlistClick = { navController.navigate(RojanDestinations.WAITLIST) },
                            )
                        }
                    }

                    composable(
                        route = RojanDestinations.WAITLIST,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        CustomerAccessGuard(
                            authViewModel = authViewModel,
                            onAccessDenied = { navController.popBackStack() },
                        ) {
                            val ecosystemViewModel = customerEcosystemViewModel
                            WaitlistScreen(
                                ecosystemViewModel = ecosystemViewModel,
                                onBackClick = { navController.popBackStack() },
                            )
                        }
                    }

                    composable(
                        route = RojanDestinations.RESCHEDULE_APPOINTMENT,
                        arguments = listOf(androidx.navigation.navArgument("appointmentId") { type = NavType.StringType }),
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        CustomerAccessGuard(
                            authViewModel = authViewModel,
                            onAccessDenied = { navController.popBackStack() },
                        ) {
                            val ecosystemViewModel = customerEcosystemViewModel
                            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""
                            RescheduleAppointmentScreen(
                                appointmentId = appointmentId,
                                ecosystemViewModel = ecosystemViewModel,
                                onBackClick = { navController.popBackStack() },
                                onRescheduled = { navController.popBackStack() },
                            )
                        }
                    }




                    composable(
                        route = RojanDestinations.APPOINTMENT_DETAILS,
                        arguments = listOf(androidx.navigation.navArgument("appointmentId") { type = NavType.StringType }),
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        CustomerAccessGuard(
                            authViewModel = authViewModel,
                            onAccessDenied = { navController.popBackStack() },
                        ) {
                            val ecosystemViewModel = customerEcosystemViewModel
                            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""
                            AppointmentDetailsScreen(
                                appointmentId = appointmentId,
                                ecosystemViewModel = ecosystemViewModel,
                                onBackClick = { navController.popBackStack() },
                                onRebookClick = { serviceId ->
                                    // Cross-graph navigation into BOOKING_FLOW_GRAPH's
                                    // ServiceDetails - navigating directly to the
                                    // destination route enters that graph correctly on
                                    // its own (same lesson learned/fixed once already
                                    // in Journey 1's own navigation wiring).
                                    navController.navigate(RojanDestinations.serviceDetails(serviceId))
                                },
                            )
                        }
                    }




                    composable(
                        route = RojanDestinations.FAVORITES,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        CustomerAccessGuard(
                            authViewModel = authViewModel,
                            onAccessDenied = { navController.popBackStack() },
                        ) {
                            val ecosystemViewModel = customerEcosystemViewModel
                            FavoritesScreen(
                                ecosystemViewModel = ecosystemViewModel,
                                onBackClick = { navController.popBackStack() },
                                onSalonClick = { salonId -> navController.navigate(RojanDestinations.salonDetails(salonId)) },
                            )
                        }
                    }




                    composable(
                        route = RojanDestinations.WALLET,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val ecosystemViewModel = customerEcosystemViewModel
                        WalletScreen(
                            ecosystemViewModel = ecosystemViewModel,
                            onBackClick = { navController.popBackStack() },
                        )
                    }




                    composable(
                        route = RojanDestinations.COUPONS,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val ecosystemViewModel = customerEcosystemViewModel
                        CouponsScreen(
                            ecosystemViewModel = ecosystemViewModel,
                            onBackClick = { navController.popBackStack() },
                        )
                    }




                    composable(
                        route = RojanDestinations.MEMBERSHIP,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val ecosystemViewModel = customerEcosystemViewModel
                        MembershipScreen(
                            ecosystemViewModel = ecosystemViewModel,
                            onBackClick = { navController.popBackStack() },
                        )
                    }




                    composable(
                        route = RojanDestinations.LOYALTY,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val ecosystemViewModel = customerEcosystemViewModel
                        LoyaltyScreen(
                            ecosystemViewModel = ecosystemViewModel,
                            onBackClick = { navController.popBackStack() },
                        )
                    }




                    composable(
                        route = RojanDestinations.MY_REVIEWS,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val ecosystemViewModel = customerEcosystemViewModel
                        MyReviewsScreen(ecosystemViewModel = ecosystemViewModel, onBackClick = { navController.popBackStack() })
                    }




                    composable(
                        route = RojanDestinations.BEAUTY_TIMELINE,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val ecosystemViewModel = customerEcosystemViewModel
                        BeautyTimelineScreen(ecosystemViewModel = ecosystemViewModel, onBackClick = { navController.popBackStack() })
                    }

                }





                composable(
                    route = RojanDestinations.MANAGER_DASHBOARD,
                    enterTransition = {
                        motionEnter
                    },
                    exitTransition = {
                        motionExit
                    }
                ) {

                    StaffAccessGuard(
                        authViewModel = authViewModel,
                        allowedRoles = RojanDestinations.MANAGER_ROLES,
                        onAccessDenied = { navController.popBackStack() },
                    ) {
                        ManagerDashboardScreen(
                            onBackClick = { navController.navigate(RojanDestinations.WELCOME) }
                        )
                    }

                }





                composable(
                    route = RojanDestinations.STYLIST_DASHBOARD,
                    enterTransition = {
                        motionEnter
                    },
                    exitTransition = {
                        motionExit
                    }
                ) {

                    StaffAccessGuard(
                        authViewModel = authViewModel,
                        allowedRoles = RojanDestinations.STYLIST_ROLES,
                        onAccessDenied = { navController.popBackStack() },
                    ) {
                        StylistDashboardScreen(
                            onBackClick = { navController.navigate(RojanDestinations.WELCOME) }
                        )
                    }

                }

            }

        }

    }

}



@Composable
private fun RestoringSessionContent() {


    PremiumBackground {


        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {


            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {


                Text(
                    text = stringResource(
                        R.string.status_restoring_session
                    ),
                    color = RojanLuxuryCaption,
                    fontSize = 13.sp
                )



                Spacer(
                    modifier = Modifier.height(12.dp)
                )



                PremiumLoadingBar(
                    modifier = Modifier.width(120.dp)
                )

            }

        }

    }

}