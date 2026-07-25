package ai.rojan.designlab.navigation

import ai.rojan.designlab.domain.model.Role

/** Centralized navigation route constants — avoids magic route strings scattered across composables. */
object RojanDestinations {
    const val WELCOME = "welcome"
    const val BOOKING_LANDING = "booking_landing"
    const val CUSTOMER_HOME = "customer_home"
    const val MANAGER_DASHBOARD = "manager_dashboard"
    const val STYLIST_DASHBOARD = "stylist_dashboard"

    // ── Journey 1: Search → Salon → Specialist → Service → Booking ──
    const val BOOKING_FLOW_GRAPH = "booking_flow_graph"
    const val SEARCH = "search"
    const val SPECIALIST_PROFILE = "specialist_profile/{specialistId}"
    const val SERVICE_DETAILS = "service_details/{serviceId}"
    const val BOOKING_DATE = "booking_date"
    const val BOOKING_TIME = "booking_time"
    const val BOOKING_CONFIRMATION = "booking_confirmation"
    const val BOOKING_SUCCESS = "booking_success"

    // ── Booking Experience Refactor: Auth (Mock) → Categories → Services → Salon List → ... ──
    const val AUTH = "auth"
    const val FIRST_TIME_NAME = "first_time_name"
    const val SERVICE_CATEGORIES = "service_categories"
    const val SERVICE_SELECTION = "service_selection/{categoryLabel}"
    const val SALON_LIST = "salon_list/{selectedServiceIds}"
    fun salonList(selectedServiceIds: List<String>) = "salon_list/${selectedServiceIds.joinToString(",")}"
    const val SPECIALIST_SELECTION = "specialist_selection/{salonId}"
    fun specialistSelection(salonId: String) = "specialist_selection/$salonId"
    // BOOKING_PAYMENT intentionally removed (Code Cleanup pass) - was
    // never wired to a composable; payment lives inside
    // BookingConfirmationScreen instead, per "Do NOT increase the
    // number of booking steps."

    fun serviceSelection(categoryLabel: String) = "service_selection/$categoryLabel"

    const val SALON_DETAILS = "salon_details/{salonId}?selectedServiceIds={selectedServiceIds}"

    fun salonDetails(salonId: String) = "salon_details/$salonId"
    fun salonDetailsWithServices(salonId: String, selectedServiceIds: List<String>) =
        "salon_details/$salonId?selectedServiceIds=${selectedServiceIds.joinToString(",")}"
    fun specialistProfile(specialistId: String) = "specialist_profile/$specialistId"
    fun serviceDetails(serviceId: String) = "service_details/$serviceId"

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

    /**
     * Maps a pure business [ai.rojan.designlab.domain.booking.BookingStep]
     * to an actual route — Navigation's job, per the "BookingContext must
     * not know Navigation" architecture decision. [ai.rojan.designlab.domain.booking.BookingStep.SALON]/
     * `SPECIALIST`/`SERVICE` don't have a dedicated *generic* "pick one"
     * screen in the current implementation (those are reached by tapping
     * a specific real entity's card, not a step in a fixed sequence) —
     * mapped to [SEARCH] as the reasonable fallback entry point, not
     * because it's semantically exact.
     */
    fun routeForBookingStep(step: ai.rojan.designlab.domain.booking.BookingStep): String = when (step) {
        ai.rojan.designlab.domain.booking.BookingStep.SEARCH,
        ai.rojan.designlab.domain.booking.BookingStep.SALON,
        ai.rojan.designlab.domain.booking.BookingStep.SPECIALIST,
        ai.rojan.designlab.domain.booking.BookingStep.SERVICE -> SEARCH
        ai.rojan.designlab.domain.booking.BookingStep.DATE -> BOOKING_DATE
        ai.rojan.designlab.domain.booking.BookingStep.TIME -> BOOKING_TIME
        ai.rojan.designlab.domain.booking.BookingStep.CONFIRMATION -> BOOKING_CONFIRMATION
        ai.rojan.designlab.domain.booking.BookingStep.SUCCESS -> BOOKING_SUCCESS
    }

    /** Resolves the dashboard/home route a given [Role] should land on. */
    fun routeForRole(role: Role): String = when (role) {
        Role.CUSTOMER -> CUSTOMER_HOME
        Role.SALON_MANAGER -> MANAGER_DASHBOARD
        Role.STYLIST -> STYLIST_DASHBOARD
    }
}