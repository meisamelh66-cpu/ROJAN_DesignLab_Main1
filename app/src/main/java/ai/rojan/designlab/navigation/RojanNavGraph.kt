package ai.rojan.designlab.navigation

import ai.rojan.designlab.R
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.components.PremiumLoadingBar
import ai.rojan.designlab.di.BackendApiContainerHolder
import ai.rojan.designlab.domain.booking.BookingIntent
import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.navigation.RojanDestinations.routeForBookingStep
import ai.rojan.designlab.presentation.booking.BookingViewModel
import ai.rojan.designlab.presentation.booking.BookingViewModelFactory
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.presentation.auth.AuthViewModelFactory
import ai.rojan.designlab.domain.identity.PersonRole
import ai.rojan.designlab.domain.identity.SessionState
import ai.rojan.designlab.presentation.session.SessionRestoreState
import ai.rojan.designlab.presentation.session.SessionViewModel
import ai.rojan.designlab.presentation.session.SessionViewModelFactory
import ai.rojan.designlab.screens.auth.AuthScreen
import ai.rojan.designlab.screens.booking.SalonListScreen
import ai.rojan.designlab.screens.booking.SpecialistSelectionScreen
import ai.rojan.designlab.screens.bookingflow.BookingConfirmationScreen
import ai.rojan.designlab.screens.bookingflow.BookingDateScreen
import ai.rojan.designlab.screens.bookingflow.BookingSuccessScreen
import ai.rojan.designlab.screens.bookingflow.BookingTimeScreen
import ai.rojan.designlab.screens.customer.CustomerDashboardScreen
import ai.rojan.designlab.screens.customer.CustomerHomeScreen
import ai.rojan.designlab.screens.customer.CustomerHomeTab
import ai.rojan.designlab.screens.profile.AppointmentDetailsScreen
import ai.rojan.designlab.screens.profile.AppointmentsScreen
import ai.rojan.designlab.screens.profile.RescheduleAppointmentScreen
import ai.rojan.designlab.screens.profile.WaitlistScreen
import ai.rojan.designlab.screens.profile.BeautyDnaScreen
import ai.rojan.designlab.screens.profile.BeautyTimelineScreen
import ai.rojan.designlab.screens.profile.CouponsScreen
import ai.rojan.designlab.screens.profile.FavoritesScreen
import ai.rojan.designlab.screens.profile.FollowedSalonsScreen
import ai.rojan.designlab.screens.profile.LoyaltyScreen
import ai.rojan.designlab.screens.profile.MembershipScreen
import ai.rojan.designlab.screens.profile.MyReviewsScreen
import ai.rojan.designlab.screens.profile.ProfileScreen
import ai.rojan.designlab.screens.profile.WalletScreen
import ai.rojan.designlab.screens.salon.PublicSalonScreen
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import androidx.navigation.NavGraph.Companion.findStartDestination
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
 * UX Refactor Phase 1: tells [RojanDestinations.AUTH] whether it was
 * reached as the mid-booking-flow "log in only when booking" gate (from
 * [RojanDestinations.BOOKING_TIME]) versus the older pre-booking entry
 * point (from [RojanDestinations.CUSTOMER_HOME]'s hero card).
 * [RojanDestinations.BOOKING_FLOW_GRAPH]'s own back-stack entry is only
 * present once that nested graph has actually been entered, and
 * navigating from BOOKING_TIME to AUTH doesn't pop it — so its presence
 * is a reliable, already-existing signal, not new state to track.
 */
private fun NavController.hasBookingFlowInProgress(): Boolean =
    runCatching { getBackStackEntry(RojanDestinations.BOOKING_FLOW_GRAPH) }.isSuccess

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

            // Authentication Session Persistence fix: a stored personId is
            // only a *claim* — it must be validated against the real
            // backend (restoreSession -> GET /api/v1/auth/me, transparently
            // refreshing the access token first) before this composable
            // decides where to route. The previous version fired
            // restoreSession fire-and-forget and computed startDestination
            // immediately from the raw, unvalidated personId, so a user
            // whose refresh token had actually expired/been revoked still
            // got routed straight to CUSTOMER_HOME for a frame before
            // things silently started failing with no "please log in
            // again" signal anywhere. This blocks on the real result
            // first ("Check stored session -> Validate/refresh token ->
            // Restore authenticated state -> Enter application").
            var isRestoringSession by remember { mutableStateOf(state.personId != null) }

            LaunchedEffect(state) {
                val personId = state.personId
                if (personId != null) {
                    authViewModel.restoreSession(personId)
                }
                isRestoringSession = false
            }

            if (isRestoringSession) {
                RestoringSessionContent()
                return
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
                        onLoginRequired = { navController.navigate(RojanDestinations.AUTH) },
                        // Android <-> Backend Full Integration milestone: the
                        // business-login entry point (Manager/Specialist role
                        // routing) is disabled — it matched a verified phone
                        // number against demo staff records, which has no
                        // real backend equivalent (no organizations/permissions
                        // concept server-side). Leaving onBusinessLoginClick
                        // unset hides the link (see SalonListScreen's own
                        // `if (onBusinessLoginClick != null)` guard) rather
                        // than wiring it to something that can never work.
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
                    //
                    // Authentication Session Persistence fix: tightened from
                    // hasBookingFlowInProgress() (true whenever BOOKING_FLOW_GRAPH
                    // exists ANYWHERE in the back stack) to the precise immediate-
                    // predecessor check the doc comment below already claimed was
                    // true — SALON_DETAILS lives inside BOOKING_FLOW_GRAPH too
                    // (Journey 1's salon browsing), so the loose check falsely
                    // matched a Follow/Favorite-triggered login from Salon Details
                    // and would have hijacked it into "resume booking" instead of
                    // just returning to Salon Details (Part 3's protected-route
                    // fix, below). BOOKING_TIME's own onTimeSelected is confirmed
                    // (by grep) to be the only real call site that reaches AUTH
                    // this way.
                    val cameFromBookingTime = navController.previousBackStackEntry?.destination?.route == RojanDestinations.BOOKING_TIME
                    val inProgressBookingViewModel = if (cameFromBookingTime) {
                        bookingViewModelFor(navController, backStackEntry)
                    } else {
                        null
                    }
                    AuthScreen(
                        authViewModel = authViewModel,
                        onBackClick = { navController.popBackStack() },
                        onExistingUserAuthenticated = {
                            // Customer Authentication Migration: personId
                            // persistence now happens inside
                            // AuthViewModel.verifyOtp itself — no bridge call
                            // needed here any more. "Login/OTP only when
                            // booking" — resumes exactly where the booking
                            // flow left off.
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
                                // Protected Route Handling fix: every
                                // CustomerAccessGuard-gated screen (Appointments,
                                // Waitlist, Reschedule, Favorites, Followed Salons)
                                // and the Salon Details Follow/Favorite buttons now
                                // reach AUTH this way — navigate(AUTH) without
                                // popping the origin first, so it's still the very
                                // next entry back. Returning to it (not a fixed
                                // destination) is what "do not lose user intent"
                                // means here: the guard re-evaluates with the now-
                                // real LoggedIn state and simply renders its real
                                // content.
                                navController.previousBackStackEntry != null -> {
                                    navController.popBackStack()
                                }
                                else -> {
                                    // Defensive fallback only — reachable if AUTH
                                    // somehow has no prior back-stack entry, which
                                    // no current call site produces.
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
                        onLoginRequired = { navController.navigate(RojanDestinations.AUTH) },
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
                            onLoginRequired = { navController.navigate(RojanDestinations.AUTH) },
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
                            selectedServiceIds = selectedServiceIds,
                            onBackClick = { navController.popBackStack() },
                            onSpecialistClick = { specialistId ->
                                navController.navigate(RojanDestinations.specialistProfile(specialistId, salonId))
                            },
                            onServiceClick = { serviceId ->
                                bookingViewModel.onSalonSelected(salonId)
                                bookingViewModel.onIntentDetected(BookingIntent.SALON)
                                navController.navigate(RojanDestinations.serviceDetails(serviceId))
                            },
                            onLoginRequired = { navController.navigate(RojanDestinations.AUTH) },
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
                        SpecialistSelectionScreen(
                            salonId = salonId,
                            onBackClick = { navController.popBackStack() },
                            onSpecialistSelected = { specialistId ->
                                bookingViewModel.onSpecialistSelected(specialistId)
                                navController.navigate(RojanDestinations.BOOKING_DATE)
                            },
                        )
                    }




                    composable(
                        route = RojanDestinations.SPECIALIST_PROFILE,
                        arguments = listOf(
                            navArgument("specialistId") { type = NavType.StringType },
                            navArgument("salonId") { type = NavType.StringType; nullable = true },
                        ),
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val bookingViewModel = bookingViewModelFor(navController, backStackEntry)
                        val specialistId = backStackEntry.arguments?.getString("specialistId") ?: ""
                        val salonIdForSpecialist = backStackEntry.arguments?.getString("salonId")
                        SpecialistProfileScreen(
                            specialistId = specialistId,
                            salonId = salonIdForSpecialist,
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
                        arguments = listOf(
                            navArgument("serviceId") { type = NavType.StringType },
                            navArgument("salonId") { type = NavType.StringType; nullable = true },
                        ),
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val bookingViewModel = bookingViewModelFor(navController, backStackEntry)
                        val serviceId = backStackEntry.arguments?.getString("serviceId") ?: ""
                        val specialistRepository = BackendApiContainerHolder.get(LocalContext.current).specialistRepository
                        val coroutineScope = rememberCoroutineScope()
                        // Phase 2 (C1): entry points that reach ServiceDetails without
                        // having walked the salon-first flow (e.g. rebooking from
                        // AppointmentDetailsScreen, outside BOOKING_FLOW_GRAPH) carry
                        // salonId as a nav arg instead - seed the fresh BookingViewModel
                        // from it exactly once so this stays the state's single source
                        // of truth downstream, same as every other entry point.
                        val salonIdArg = backStackEntry.arguments?.getString("salonId")
                        if (bookingViewModel.state.salonId == null && salonIdArg != null) {
                            bookingViewModel.onSalonSelected(salonIdArg)
                        }
                        ServiceDetailsScreen(
                            serviceId = serviceId,
                            salonId = bookingViewModel.state.salonId,
                            onBackClick = { navController.popBackStack() },
                            onBookClick = {
                                bookingViewModel.onServiceSelected(serviceId)
                                if (bookingViewModel.state.intent == BookingIntent.UNKNOWN) {
                                    bookingViewModel.onIntentDetected(BookingIntent.SERVICE)
                                }
                                coroutineScope.launch {
                                    // Customer Journey Audit Phase A (P0-1) fix:
                                    // auto-select the specialist when the salon
                                    // only has one — mirrors SalonDetailsScreen's
                                    // existing onContinueBooking behavior — so
                                    // BookingStepResolver only asks the customer
                                    // when there's a genuine choice to make.
                                    // Production Data Integrity Phase 1: sourced
                                    // from the real, salon-scoped SpecialistRepository
                                    // instead of CatalogEngine — single lookup by a
                                    // known salonId, not a cross-salon listing, so
                                    // this is a real migration, not an N+1 case.
                                    if (bookingViewModel.state.specialistId == null) {
                                        val specialists = bookingViewModel.state.salonId
                                            ?.let { specialistRepository.getSpecialists(it).getOrNull() }
                                            ?: emptyList()
                                        if (specialists.size == 1) {
                                            bookingViewModel.onSpecialistSelected(specialists.first().id)
                                        }
                                    }
                                    navController.navigate(
                                        routeForBookingStep(bookingViewModel.nextStep(), bookingViewModel.state.salonId)
                                    )
                                }
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
                            dateKey = bookingViewModel.state.selectedDateKey
                                ?: ai.rojan.designlab.domain.booking.RollingBookingDates.next7Days().first().first,
                            bookingViewModel = bookingViewModel,
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
                            onConfirmClick = { _, _ ->
                                // Production Data Integrity Phase 1 (Task 7): the
                                // real booking already exists on the backend
                                // (BookingConfirmationViewModel's POST
                                // /api/v1/bookings) by the time this fires -
                                // AppointmentsScreen/UpcomingBookings/RecentVisits
                                // now read it back directly via
                                // BookingHistoryRepository, so recording it a
                                // second time into the local, now-gated
                                // CustomerEcosystemViewModel is no longer needed.
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
                        authViewModel = authViewModel,
                        onProfileClick = { navController.navigate(RojanDestinations.PROFILE) },
                        onBookAppointmentClick = { navController.navigate(RojanDestinations.MEMBER_SALONS_LIST) },
                        onBookingsClick = { navController.navigate(RojanDestinations.APPOINTMENTS) },
                        onFavoritesClick = { navController.navigate(RojanDestinations.FAVORITES) },
                        onExploreClick = { navController.navigate(RojanDestinations.EXPLORE) },
                        onSearchClick = { navController.navigate(RojanDestinations.SEARCH) },
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
                        onViewAllServicesClick = { navController.navigate(RojanDestinations.MEMBER_SALONS_LIST) },
                        onSpecialistClick = { specialistId ->
                            navController.navigate(RojanDestinations.specialistProfile(specialistId))
                        },
                    )
                }




                // Public Salon Activation Phase 1: in-app route only (no deep
                // link registered yet, per approved scope) - top-level, no
                // CustomerAccessGuard, since this is the one deliberately
                // unauthenticated screen in the app (see PublicSalonScreen's
                // own doc comment). Does not touch BOOKING_FLOW_GRAPH/
                // BookingViewModel - the screen is read-only, its only
                // action is the login CTA below.
                composable(
                    route = RojanDestinations.PUBLIC_SALON,
                    arguments = listOf(navArgument("slug") { type = NavType.StringType }),
                    enterTransition = { motionEnter },
                    exitTransition = { motionExit },
                ) { backStackEntry ->
                    val slug = backStackEntry.arguments?.getString("slug") ?: ""
                    PublicSalonScreen(
                        slug = slug,
                        onBackClick = { navController.popBackStack() },
                        onLoginClick = { navController.navigate(RojanDestinations.AUTH) },
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
                        ProfileScreen(
                            authViewModel = authViewModel,
                            onBackClick = { navController.popBackStack() },
                            onBeautyDnaClick = { navController.navigate(RojanDestinations.BEAUTY_DNA) },
                            onAppointmentsClick = { navController.navigate(RojanDestinations.APPOINTMENTS) },
                            onFollowedSalonsClick = { navController.navigate(RojanDestinations.FOLLOWED_SALONS) },
                            onFavoritesClick = { navController.navigate(RojanDestinations.FAVORITES) },
                            onWalletClick = { navController.navigate(RojanDestinations.WALLET) },
                            onCouponsClick = { navController.navigate(RojanDestinations.COUPONS) },
                            onMembershipClick = { navController.navigate(RojanDestinations.MEMBERSHIP) },
                            onLoyaltyClick = { navController.navigate(RojanDestinations.LOYALTY) },
                            onReviewsClick = { navController.navigate(RojanDestinations.MY_REVIEWS) },
                            onBeautyTimelineClick = { navController.navigate(RojanDestinations.BEAUTY_TIMELINE) },
                            onLogoutClick = {
                                authViewModel.logout()
                                navController.navigate(RojanDestinations.EXPLORE) {
                                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }




                    composable(
                        route = RojanDestinations.APPOINTMENTS,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        CustomerAccessGuard(
                            authViewModel = authViewModel,
                            onAccessDenied = { navController.navigate(RojanDestinations.AUTH) },
                        ) {
                            AppointmentsScreen(
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
                            onAccessDenied = { navController.navigate(RojanDestinations.AUTH) },
                        ) {
                            WaitlistScreen(
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
                            onAccessDenied = { navController.navigate(RojanDestinations.AUTH) },
                        ) {
                            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""
                            RescheduleAppointmentScreen(
                                appointmentId = appointmentId,
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
                            onAccessDenied = { navController.navigate(RojanDestinations.AUTH) },
                        ) {
                            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""
                            AppointmentDetailsScreen(
                                appointmentId = appointmentId,
                                onBackClick = { navController.popBackStack() },
                                onRebookClick = { serviceId, salonId ->
                                    // Cross-graph navigation into BOOKING_FLOW_GRAPH's
                                    // ServiceDetails - navigating directly to the
                                    // destination route enters that graph correctly on
                                    // its own (same lesson learned/fixed once already
                                    // in Journey 1's own navigation wiring).
                                    //
                                    // Phase 2 (C1): salonId now travels as a nav arg -
                                    // the real Booking already carries it (unlike the
                                    // old demo appointment model this bug predates), so
                                    // there's no reason to make the customer re-pick a
                                    // salon ServiceDetailsScreen already knows. The
                                    // SERVICE_DETAILS composable seeds the fresh
                                    // BookingViewModel from this arg.
                                    navController.navigate(RojanDestinations.serviceDetails(serviceId, salonId))
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
                            onAccessDenied = { navController.navigate(RojanDestinations.AUTH) },
                        ) {
                            FavoritesScreen(
                                onBackClick = { navController.popBackStack() },
                                onSalonClick = { salonId -> navController.navigate(RojanDestinations.salonDetails(salonId)) },
                            )
                        }
                    }

                    composable(
                        route = RojanDestinations.FOLLOWED_SALONS,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        CustomerAccessGuard(
                            authViewModel = authViewModel,
                            onAccessDenied = { navController.navigate(RojanDestinations.AUTH) },
                        ) {
                            FollowedSalonsScreen(
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
                        WalletScreen(
                            onBackClick = { navController.popBackStack() },
                        )
                    }




                    composable(
                        route = RojanDestinations.COUPONS,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        CouponsScreen(
                            onBackClick = { navController.popBackStack() },
                        )
                    }




                    composable(
                        route = RojanDestinations.MEMBERSHIP,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        MembershipScreen(
                            onBackClick = { navController.popBackStack() },
                        )
                    }




                    composable(
                        route = RojanDestinations.LOYALTY,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        LoyaltyScreen(
                            onBackClick = { navController.popBackStack() },
                        )
                    }




                    composable(
                        route = RojanDestinations.MY_REVIEWS,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        MyReviewsScreen(onBackClick = { navController.popBackStack() })
                    }




                    composable(
                        route = RojanDestinations.BEAUTY_TIMELINE,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        BeautyTimelineScreen(onBackClick = { navController.popBackStack() })
                    }




                    composable(
                        route = RojanDestinations.BEAUTY_DNA,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
                        BeautyDnaScreen(
                            customerId = currentUser?.id ?: "",
                            beautyProfileRepository = BackendApiContainerHolder.get(LocalContext.current).beautyProfileRepository,
                            onBackClick = { navController.popBackStack() },
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