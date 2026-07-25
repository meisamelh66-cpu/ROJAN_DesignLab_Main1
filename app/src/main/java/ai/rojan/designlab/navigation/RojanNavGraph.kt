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
import ai.rojan.designlab.presentation.roleselection.RoleSelectionViewModel
import ai.rojan.designlab.presentation.roleselection.RoleSelectionViewModelFactory
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.presentation.auth.AuthViewModelFactory
import ai.rojan.designlab.domain.model.Role
import ai.rojan.designlab.presentation.session.SessionRestoreState
import ai.rojan.designlab.presentation.session.SessionViewModel
import ai.rojan.designlab.presentation.session.SessionViewModelFactory
import ai.rojan.designlab.screens.auth.AuthScreen
import ai.rojan.designlab.screens.auth.FirstTimeNameScreen
import ai.rojan.designlab.screens.booking.BookingLandingScreen
import ai.rojan.designlab.screens.booking.SalonListScreen
import ai.rojan.designlab.screens.booking.ServiceCategoriesScreen
import ai.rojan.designlab.screens.booking.ServiceSelectionScreen
import ai.rojan.designlab.screens.booking.SpecialistSelectionScreen
import ai.rojan.designlab.screens.bookingflow.BookingConfirmationScreen
import ai.rojan.designlab.screens.bookingflow.BookingDateScreen
import ai.rojan.designlab.screens.bookingflow.BookingSuccessScreen
import ai.rojan.designlab.screens.bookingflow.BookingTimeScreen
import ai.rojan.designlab.screens.customer.CustomerHomeScreen
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

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
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


private val motionEnter =
    fadeIn(
        animationSpec = tween(280)
    ) +
            scaleIn(
                initialScale = 0.97f,
                animationSpec = tween(280)
            )


private val motionExit =
    fadeOut(
        animationSpec = tween(160)
    )


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
    return viewModel(
        viewModelStoreOwner = parentEntry,
        factory = BookingViewModelFactory(),
    )
}

/**
 * Obtains the single [CustomerEcosystemViewModel] instance shared by
 * every screen inside the [RojanDestinations.PROFILE_GRAPH] sub-graph —
 * identical pattern and reasoning to [bookingViewModelFor].
 */
@Composable
private fun customerEcosystemViewModelFor(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
): CustomerEcosystemViewModel {
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(RojanDestinations.PROFILE_GRAPH)
    }
    return viewModel(
        viewModelStoreOwner = parentEntry,
        factory = CustomerEcosystemViewModelFactory(),
    )
}

/**
 * Production Readiness Audit (V1.0 Module 6): real navigation guard for
 * Customer-only screens (Appointments, Reschedule, Waiting List).
 *
 * Deliberately checks [SessionViewModel]'s [ai.rojan.designlab.domain.model.Role]
 * (the OLD role-selection system that actually governs this app's
 * top-level navigation today - `state.role?.let { routeForRole(it) }`
 * as the graph's `startDestination`) rather than the NEW Identity
 * Foundation's `PersonRole` system. This is a deliberate choice, not an
 * oversight: the two role systems are currently parallel and
 * unreconciled (a real, disclosed architectural gap) - a customer who
 * reaches these screens via the OLD role-selection flow (the app's
 * primary, most common path today) has never authenticated through the
 * NEW phone/OTP system at all, so guarding against `PersonRole` here
 * would incorrectly block the majority of legitimate customers, a real
 * regression, not a fix. Guarding against the OLD `Role` (which
 * genuinely does gate SALON_MANAGER/STYLIST away from
 * CUSTOMER_HOME already) is the correct, safe check for what this audit
 * actually asked to prevent: "unauthorized roles cannot access Customer
 * appointment screens."
 *
 * [SessionRestoreState.Loading] allows content through rather than
 * blocking - the first frame(s) after launch, not a real denial case;
 * blocking here would flash a false access-denied state on every cold
 * start.
 */
@Composable
private fun CustomerAccessGuard(
    sessionViewModel: SessionViewModel,
    onAccessDenied: () -> Unit,
    content: @Composable () -> Unit,
) {
    val restoreState by sessionViewModel.restoreState.collectAsStateWithLifecycle()
    when (val state = restoreState) {
        is SessionRestoreState.Loading -> content()
        is SessionRestoreState.Restored -> {
            if (state.role == null || state.role == Role.CUSTOMER) {
                content()
            } else {
                LaunchedEffect(Unit) { onAccessDenied() }
            }
        }
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
        factory = AuthViewModelFactory()
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


            NavHost(
                navController = navController,
                startDestination = state.role?.let { RojanDestinations.routeForRole(it) }
                    ?: RojanDestinations.WELCOME
            ) {



                composable(
                    route = RojanDestinations.WELCOME,
                    enterTransition = {
                        motionEnter
                    },
                    exitTransition = {
                        motionExit
                    }
                ) {


                    val roleSelectionViewModel: RoleSelectionViewModel =
                        viewModel(
                            factory = RoleSelectionViewModelFactory(appContext)
                        )


                    WelcomeRoute(
                        roleSelectionViewModel = roleSelectionViewModel,
                        navController = navController
                    )

                }




                composable(
                    route = RojanDestinations.AUTH,
                    enterTransition = { motionEnter },
                    exitTransition = { motionExit },
                ) {
                    AuthScreen(
                        authViewModel = authViewModel,
                        onBackClick = { navController.popBackStack() },
                        onExistingUserAuthenticated = {
                            navController.navigate(RojanDestinations.SERVICE_CATEGORIES) {
                                popUpTo(RojanDestinations.AUTH) { inclusive = true }
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
                ) {
                    FirstTimeNameScreen(
                        authViewModel = authViewModel,
                        onNameSubmitted = {
                            navController.navigate(RojanDestinations.SERVICE_CATEGORIES) {
                                popUpTo(RojanDestinations.AUTH) { inclusive = true }
                            }
                        },
                    )
                }

                composable(
                    route = RojanDestinations.SERVICE_CATEGORIES,
                    enterTransition = { motionEnter },
                    exitTransition = { motionExit },
                ) {
                    ServiceCategoriesScreen(
                        onBackClick = { navController.popBackStack() },
                        onCategorySelected = { categoryLabel ->
                            navController.navigate(RojanDestinations.serviceSelection(categoryLabel))
                        },
                    )
                }

                composable(
                    route = RojanDestinations.SERVICE_SELECTION,
                    arguments = listOf(navArgument("categoryLabel") { type = NavType.StringType }),
                    enterTransition = { motionEnter },
                    exitTransition = { motionExit },
                ) { backStackEntry ->
                    val categoryLabel = backStackEntry.arguments?.getString("categoryLabel") ?: ""
                    ServiceSelectionScreen(
                        categoryLabel = categoryLabel,
                        onBackClick = { navController.popBackStack() },
                        onContinueBooking = { selectedServiceIds ->
                            navController.navigate(RojanDestinations.salonList(selectedServiceIds))
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

                composable(
                    route = RojanDestinations.BOOKING_LANDING,
                    enterTransition = {
                        motionEnter
                    },
                    exitTransition = {
                        motionExit
                    }
                ) {

                    BookingLandingScreen(
                        onSearchClick = { navController.navigate(RojanDestinations.BOOKING_FLOW_GRAPH) },
                        onSalonClick = { salonId ->
                            navController.navigate(RojanDestinations.salonDetails(salonId))
                        },
                        onSpecialistClick = { specialistId ->
                            navController.navigate(RojanDestinations.specialistProfile(specialistId))
                        },
                        onHomeClick = {
                            navController.navigate(RojanDestinations.CUSTOMER_HOME) {
                                popUpTo(RojanDestinations.CUSTOMER_HOME) { inclusive = false }
                            }
                        },
                        onBookingsClick = { navController.navigate(RojanDestinations.APPOINTMENTS) },
                        onProfileClick = { navController.navigate(RojanDestinations.PROFILE) },
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
                        SpecialistProfileScreen(
                            specialistId = specialistId,
                            onBackClick = { navController.popBackStack() },
                            onServiceClick = { serviceId ->
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
                        ServiceDetailsScreen(
                            serviceId = serviceId,
                            onBackClick = { navController.popBackStack() },
                            onBookClick = {
                                bookingViewModel.onServiceSelected(serviceId)
                                if (bookingViewModel.state.intent == BookingIntent.UNKNOWN) {
                                    bookingViewModel.onIntentDetected(BookingIntent.SERVICE)
                                }
                                navController.navigate(
                                    routeForBookingStep(bookingViewModel.nextStep())
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
                                    routeForBookingStep(bookingViewModel.nextStep())
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
                        val timeScreenEcosystemViewModel: CustomerEcosystemViewModel = viewModel(
                            viewModelStoreOwner = backStackEntry,
                            factory = CustomerEcosystemViewModelFactory(),
                        )
                        BookingTimeScreen(
                            dateKey = bookingViewModel.state.selectedDateKey ?: "today",
                            bookingViewModel = bookingViewModel,
                            ecosystemViewModel = timeScreenEcosystemViewModel,
                            onBackClick = { navController.popBackStack() },
                            onTimeSelected = { time ->
                                bookingViewModel.onTimeSelected(time)
                                navController.navigate(
                                    routeForBookingStep(bookingViewModel.nextStep())
                                )
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
                            onConfirmClick = { navController.navigate(RojanDestinations.BOOKING_SUCCESS) },
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
                                navController.navigate(RojanDestinations.BOOKING_LANDING) {
                                    popUpTo(RojanDestinations.BOOKING_LANDING) { inclusive = false }
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
                ) { backStackEntry ->
                    val homeEcosystemViewModel: CustomerEcosystemViewModel = viewModel(
                        viewModelStoreOwner = backStackEntry,
                        factory = CustomerEcosystemViewModelFactory(),
                    )
                    CustomerHomeScreen(
                        ecosystemViewModel = homeEcosystemViewModel,
                        onProfileClick = { navController.navigate(RojanDestinations.PROFILE) },
                        onBookAppointmentClick = { navController.navigate(RojanDestinations.AUTH) },
                        onBookingsClick = { navController.navigate(RojanDestinations.APPOINTMENTS) },
                        onFavoritesClick = { navController.navigate(RojanDestinations.FAVORITES) },
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
                        val ecosystemViewModel = customerEcosystemViewModelFor(navController, backStackEntry)
                        ProfileScreen(
                            ecosystemViewModel = ecosystemViewModel,
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
                            sessionViewModel = sessionViewModel,
                            onAccessDenied = { navController.popBackStack() },
                        ) {
                            val ecosystemViewModel = customerEcosystemViewModelFor(navController, backStackEntry)
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
                            sessionViewModel = sessionViewModel,
                            onAccessDenied = { navController.popBackStack() },
                        ) {
                            val ecosystemViewModel = customerEcosystemViewModelFor(navController, backStackEntry)
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
                            sessionViewModel = sessionViewModel,
                            onAccessDenied = { navController.popBackStack() },
                        ) {
                            val ecosystemViewModel = customerEcosystemViewModelFor(navController, backStackEntry)
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
                        val ecosystemViewModel = customerEcosystemViewModelFor(navController, backStackEntry)
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




                    composable(
                        route = RojanDestinations.FAVORITES,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val ecosystemViewModel = customerEcosystemViewModelFor(navController, backStackEntry)
                        FavoritesScreen(
                            ecosystemViewModel = ecosystemViewModel,
                            onBackClick = { navController.popBackStack() },
                            onSalonClick = { salonId -> navController.navigate(RojanDestinations.salonDetails(salonId)) },
                        )
                    }




                    composable(
                        route = RojanDestinations.WALLET,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val ecosystemViewModel = customerEcosystemViewModelFor(navController, backStackEntry)
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
                        val ecosystemViewModel = customerEcosystemViewModelFor(navController, backStackEntry)
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
                        val ecosystemViewModel = customerEcosystemViewModelFor(navController, backStackEntry)
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
                        val ecosystemViewModel = customerEcosystemViewModelFor(navController, backStackEntry)
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
                        val ecosystemViewModel = customerEcosystemViewModelFor(navController, backStackEntry)
                        MyReviewsScreen(ecosystemViewModel = ecosystemViewModel, onBackClick = { navController.popBackStack() })
                    }




                    composable(
                        route = RojanDestinations.BEAUTY_TIMELINE,
                        enterTransition = { motionEnter },
                        exitTransition = { motionExit },
                    ) { backStackEntry ->
                        val ecosystemViewModel = customerEcosystemViewModelFor(navController, backStackEntry)
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

                    ManagerDashboardScreen(
                        onBackClick = { navController.navigate(RojanDestinations.WELCOME) }
                    )

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

                    StylistDashboardScreen(
                        onBackClick = { navController.navigate(RojanDestinations.WELCOME) }
                    )

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