package ai.rojan.designlab.navigation

import ai.rojan.designlab.domain.identity.PersonRole

/** Centralized navigation route constants — avoids magic route strings scattered across composables. */
object RojanDestinations {
    const val WELCOME = "welcome"
    // UX Refactor Phase 1: the default entry point for a customer with no
    // persisted Role yet — an unfiltered, unauthenticated salon browse
    // list, per the confirmed "Splash → Member Salons List" new-customer
    // flow. WELCOME still exists but is now a secondary entry point
    // (Manager/Specialist "business login" only), not the default landing.
    const val MEMBER_SALONS_LIST = "member_salons_list"
    // UX Correction (Explore Repositioning): CUSTOMER_HOME now renders
    // CustomerDashboardScreen (personalized greeting, hero CTA, upcoming
    // appointments, AI recommendations, suggested specialists) — the new
    // first-impression screen after auth. The former CUSTOMER_HOME content
    // (search + full marketplace discovery: services/specialists/salons)
    // is unchanged but moved to EXPLORE, reached from the Dashboard rather
    // than being the first thing a customer sees.
    const val CUSTOMER_HOME = "customer_home"
    const val EXPLORE = "explore"
    const val MANAGER_DASHBOARD = "manager_dashboard"
    const val STYLIST_DASHBOARD = "stylist_dashboard"

    // ── Journey 1: Search → Salon → Specialist → Service → Booking ──
    const val BOOKING_FLOW_GRAPH = "booking_flow_graph"
    const val SEARCH = "search"
    // Android <-> Backend Full Integration milestone: salonId is optional
    // because the backend has no "get specialist by id alone" endpoint
    // (always salon-scoped) — callers that know the salon (Salon Details)
    // pass it; callers that don't (the Home dashboard's Top Specialists
    // widget, still demo-content/out of this milestone's scope) omit it,
    // and SpecialistProfileScreen shows a disclosed "no salon" error
    // rather than guessing.
    const val SPECIALIST_PROFILE = "specialist_profile/{specialistId}?salonId={salonId}"
    const val SERVICE_DETAILS = "service_details/{serviceId}?salonId={salonId}"
    const val BOOKING_DATE = "booking_date"
    const val BOOKING_TIME = "booking_time"
    const val BOOKING_CONFIRMATION = "booking_confirmation"
    const val BOOKING_SUCCESS = "booking_success"

    // ── Login/OTP (only when booking) + salon-first booking flow ──
    // UX Refactor Phase 1: the category-first pre-salon pickers
    // (SERVICE_CATEGORIES/SERVICE_SELECTION) were removed — both target
    // flows pick the salon first, then narrow services within it.
    const val AUTH = "auth"
    const val FIRST_TIME_NAME = "first_time_name"
    const val SALON_LIST = "salon_list/{selectedServiceIds}"
    fun salonList(selectedServiceIds: List<String>) = "salon_list/${selectedServiceIds.joinToString(",")}"
    const val SPECIALIST_SELECTION = "specialist_selection/{salonId}"
    fun specialistSelection(salonId: String) = "specialist_selection/$salonId"
    // BOOKING_PAYMENT intentionally removed (Code Cleanup pass) - was
    // never wired to a composable; payment lives inside
    // BookingConfirmationScreen instead, per "Do NOT increase the
    // number of booking steps."

    const val SALON_DETAILS = "salon_details/{salonId}?selectedServiceIds={selectedServiceIds}"

    fun salonDetails(salonId: String) = "salon_details/$salonId"
    fun salonDetailsWithServices(salonId: String, selectedServiceIds: List<String>) =
        "salon_details/$salonId?selectedServiceIds=${selectedServiceIds.joinToString(",")}"
    fun specialistProfile(specialistId: String, salonId: String? = null) =
        "specialist_profile/$specialistId" + if (salonId != null) "?salonId=$salonId" else ""
    fun serviceDetails(serviceId: String, salonId: String? = null) =
        "service_details/$serviceId" + if (salonId != null) "?salonId=$salonId" else ""

    // ── Journey 2: Home → Profile → Appointments/Favorites/Wallet/... ──
    const val PROFILE_GRAPH = "profile_graph"
    const val PROFILE = "profile"
    const val APPOINTMENT_DETAILS = "appointment_details/{appointmentId}"
    fun appointmentDetails(appointmentId: String) = "appointment_details/$appointmentId"
    const val APPOINTMENTS = "appointments"
    const val WAITLIST = "waitlist"
    const val RESCHEDULE_APPOINTMENT = "reschedule_appointment/{appointmentId}"
    fun rescheduleAppointment(appointmentId: String) = "reschedule_appointment/$appointmentId"
    const val FAVORITES = "favorites"
    const val WALLET = "wallet"
    const val COUPONS = "coupons"
    const val MEMBERSHIP = "membership"
    const val LOYALTY = "loyalty"
    const val MY_REVIEWS = "my_reviews"
    const val BEAUTY_TIMELINE = "beauty_timeline"
    const val BEAUTY_DNA = "beauty_dna"

    /**
     * Maps a pure business [ai.rojan.designlab.domain.booking.BookingStep]
     * to an actual route — Navigation's job, per the "BookingContext must
     * not know Navigation" architecture decision. [ai.rojan.designlab.domain.booking.BookingStep.SALON]/
     * `SERVICE` don't have a dedicated *generic* "pick one" screen in the
     * current implementation (those are reached by tapping a specific
     * real entity's card, not a step in a fixed sequence) — mapped to
     * [SEARCH] as the reasonable fallback entry point, not because it's
     * semantically exact.
     *
     * Customer Journey Audit Phase A (P0-1) fix: [ai.rojan.designlab.domain.booking.BookingStep.SPECIALIST]
     * now routes to the real [SPECIALIST_SELECTION] screen (it used to
     * fall back to [SEARCH] like SALON/SERVICE above, silently skipping
     * specialist choice for every customer) — needs [salonId] to build
     * that parametrized route. Falls back to [SEARCH] only if [salonId]
     * is somehow unset, which shouldn't happen by the time this step is
     * reached (defensive, not the expected path).
     */
    fun routeForBookingStep(step: ai.rojan.designlab.domain.booking.BookingStep, salonId: String?): String = when (step) {
        ai.rojan.designlab.domain.booking.BookingStep.SPECIALIST ->
            salonId?.let { specialistSelection(it) } ?: SEARCH
        ai.rojan.designlab.domain.booking.BookingStep.SEARCH,
        ai.rojan.designlab.domain.booking.BookingStep.SALON,
        ai.rojan.designlab.domain.booking.BookingStep.SERVICE -> SEARCH
        ai.rojan.designlab.domain.booking.BookingStep.DATE -> BOOKING_DATE
        ai.rojan.designlab.domain.booking.BookingStep.TIME -> BOOKING_TIME
        ai.rojan.designlab.domain.booking.BookingStep.CONFIRMATION -> BOOKING_CONFIRMATION
        ai.rojan.designlab.domain.booking.BookingStep.SUCCESS -> BOOKING_SUCCESS
    }

    // UX Refactor Phase 3: real, per-salon PersonRole assignments (not a
    // card tap) now decide staff access. Back-office roles all land on
    // the one existing Manager dashboard placeholder — this phase doesn't
    // build differentiated dashboards per role, only real access to the
    // existing ones.
    val MANAGER_ROLES = setOf(
        PersonRole.OWNER,
        PersonRole.GENERAL_MANAGER,
        PersonRole.RECEPTION,
        PersonRole.FINANCE,
        PersonRole.HR,
    )
    val STYLIST_ROLES = setOf(PersonRole.SPECIALIST)

    /**
     * Resolves the staff dashboard route for a set of real [PersonRole]s,
     * or `null` if none of them grant staff access at all (e.g. a
     * customer-only account, or nobody logged in). Back-office roles take
     * priority if someone somehow holds both a [MANAGER_ROLES] and a
     * [STYLIST_ROLES] role at once — an arbitrary but documented choice;
     * no seeded demo account exercises this today.
     */
    fun routeForPersonRoles(roles: Set<PersonRole>): String? = when {
        roles.any { it in MANAGER_ROLES } -> MANAGER_DASHBOARD
        roles.any { it in STYLIST_ROLES } -> STYLIST_DASHBOARD
        else -> null
    }
}