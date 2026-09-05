package ai.rojan.designlab.manager.presentation.booking

import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.booking.ManagerBookingState
import ai.rojan.designlab.manager.domain.booking.managerBookingTimeSlots
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.specialist.Specialist
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager Booking Journey Phase 2 — owns the in-progress
 * [ManagerBookingState] for the whole 7-screen wizard and every piece of
 * business logic the wizard needs (available-time computation,
 * appointment creation). Screens read [uiState] and call these methods;
 * none of them touch a repository or compute anything themselves, per
 * "no business logic inside Composables."
 *
 * Scoped to the wizard's own back-stack lifetime (shared across all 7
 * screens via `navController.getBackStackEntry(ManagerDestinations.CREATE_APPOINTMENT)`,
 * see [ai.rojan.designlab.manager.navigation.ManagerNavGraph]) — not an
 * app-lifetime singleton, so it's naturally cleared when the wizard
 * completes or is abandoned.
 */
class ManagerBookingViewModel(
    private val customerRepository: CustomerRepository,
    private val serviceRepository: ServiceRepository,
    private val specialistRepository: SpecialistRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManagerBookingState())
    val uiState: StateFlow<ManagerBookingState> = _uiState.asStateFlow()

    fun reset() {
        _uiState.value = ManagerBookingState()
    }

    fun selectCustomer(customerId: String) {
        _uiState.value = _uiState.value.copy(customerId = customerId)
    }

    fun selectService(serviceId: String) {
        _uiState.value = _uiState.value.copy(serviceId = serviceId)
    }

    fun selectSpecialist(specialistId: String) {
        _uiState.value = _uiState.value.copy(specialistId = specialistId)
    }

    fun selectDate(dateKey: String) {
        // Changing the date invalidates a previously chosen time — the
        // slot may not even exist/be free on the new date.
        _uiState.value = _uiState.value.copy(dateKey = dateKey, time = null)
    }

    fun selectTime(time: String) {
        _uiState.value = _uiState.value.copy(time = time)
    }

    fun searchCustomers(query: String) = customerRepository.search(query)

    fun activeServices() = serviceRepository.getAll().filter { it.active }

    fun activeSpecialists() = specialistRepository.getAll().filter { it.active }

    /**
     * Specialists whose declared skills cover [serviceName], falling
     * back to the full active roster if none match — a genuine filter,
     * but one that never dead-ends the wizard with an empty list.
     */
    fun specialistsFor(serviceName: String?): List<Specialist> {
        val active = activeSpecialists()
        if (serviceName == null) return active
        val matching = active.filter { specialist -> specialist.skills.any { it == serviceName } }
        return matching.ifEmpty { active }
    }

    fun customerById(id: String?) = id?.let { customerRepository.getById(it) }
    fun serviceById(id: String?) = id?.let { serviceRepository.getById(it) }
    fun specialistById(id: String?) = id?.let { specialistRepository.getById(it) }

    /** Free slots for [specialistId] on [dateKey] — the fixed grid minus that specialist's already-booked, non-cancelled times that day. */
    fun availableTimes(specialistId: String, dateKey: String): List<String> {
        val taken = appointmentRepository.getAll()
            .filter {
                it.specialistId == specialistId &&
                    it.date == dateKey &&
                    it.status != AppointmentStatus.CANCELLED
            }
            .map { it.time }
            .toSet()
        return managerBookingTimeSlots.filterNot { it in taken }
    }

    /**
     * TEAM2 Booking Creation Integrity follow-up. Does **not** create a
     * real backend booking, and no longer creates a local-only
     * [ai.rojan.designlab.manager.domain.appointment.Appointment]
     * pretending to be one either — that local "success" is exactly the
     * regression this follow-up fixes: TEAM2-002 made Manager Calendar
     * read real backend data, so a locally-created appointment would
     * show a success screen and then silently never appear anywhere.
     *
     * The real blocker, confirmed from source, not assumed: the
     * backend's `POST /api/v1/bookings` always attributes the booking to
     * the *authenticated caller*
     * (`BookingController.create`'s `currentUserResolver.resolve(principal)`)
     * — there is no field on `CreateBookingRequest` and no code path in
     * `CreateBookingUseCase` for a salon owner to create a booking on
     * behalf of a different customer. There is also no backend endpoint
     * at all that lets an owner look up an existing customer
     * (`UserController` exposes only `GET /users/me`), so even if the
     * first gap were closed, "select a real customer" still couldn't be
     * backed by anything real. Per this task's explicit instruction not
     * to invent a missing API or fake a customer identity, this reports
     * that honestly via [ManagerBookingState.submitError] instead of
     * either. See `TEAM2_RESULT_MANAGER_BOOKING_CREATION.md`.
     *
     * Returns `false` (always, until that backend contract exists)
     * rather than `Unit`, so the caller
     * ([ai.rojan.designlab.manager.screens.booking.ManagerBookingReviewScreen])
     * has an explicit, unmissable signal to never proceed to the success
     * screen — the same "only navigate on a real confirmed result"
     * contract TEAM2-001 established for the Customer booking flow.
     */
    fun confirm(): Boolean {
        val state = _uiState.value
        if (!state.isReadyToConfirm) return false

        _uiState.value = state.copy(
            submitError = "ثبت نوبت به نام مشتری هنوز از طریق سرور پشتیبانی نمی‌شود. این قابلیت به‌زودی اضافه خواهد شد.",
        )
        return false
    }
}
