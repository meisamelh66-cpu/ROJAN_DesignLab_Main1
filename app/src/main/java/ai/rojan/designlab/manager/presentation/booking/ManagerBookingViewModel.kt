package ai.rojan.designlab.manager.presentation.booking

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.manager.data.ManagerRepositories
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.ManagerCalendarWeek
import ai.rojan.designlab.manager.domain.booking.ManagerBookingState
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.specialist.Specialist
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 *
 * Availability/salon-id (Final Release Validation — Real Booking
 * Calendar Integration) are read live from [ManagerRepositories] in
 * [availableTimes]/[confirm], not snapshotted at construction like the
 * four repositories above: this ViewModel is built once, synchronously,
 * the moment the wizard's first screen composes — well before the user
 * has picked a customer/service/specialist and reached the date/time
 * step, [ManagerRepositories.initialize] (re-triggered on wizard entry by
 * [ai.rojan.designlab.manager.screens.booking.ManagerBookingStartScreen])
 * has realistically had time to resolve `salonId` by then, and reading it
 * live means this doesn't matter either way.
 */
class ManagerBookingViewModel(
    private val customerRepository: CustomerRepository,
    private val serviceRepository: ServiceRepository,
    private val specialistRepository: SpecialistRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManagerBookingState())
    val uiState: StateFlow<ManagerBookingState> = _uiState.asStateFlow()

    var isSubmitting by mutableStateOf(false)
        private set

    var submitError by mutableStateOf<String?>(null)
        private set

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

    /** [time] must be a raw ISO-8601 `start` value from a real [availableTimes] result — see [ManagerBookingState.time]'s doc comment for why. */
    fun selectTime(time: String) {
        _uiState.value = _uiState.value.copy(time = time)
    }

    fun searchCustomers(query: String) = customerRepository.search(query)

    fun activeServices() = serviceRepository.getAll().filter { it.active }

    fun activeSpecialists() = specialistRepository.getAll().filter { it.active }

    /**
     * Specialists whose declared skills cover [serviceName], falling
     * back to the full active roster if none match — a genuine filter,
     * but one that never dead-ends the wizard with an empty list. Every
     * real specialist's `skills` is currently an empty list (no such
     * field on the backend — see [ai.rojan.designlab.manager.data.BackendSpecialistRepository]),
     * so this always falls back to the full roster for now; the filter
     * stays in place for if/when a real skills field exists.
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

    /**
     * Real bookable windows for [specialistId] on [dateKey], computed by
     * the backend (`AvailabilityController` — considers the specialist's
     * schedule and existing bookings) — replaces the previous fixed-grid-
     * minus-taken-slots local approximation entirely. Requires a service
     * to already be selected, which the wizard's screen order (service
     * before specialist before date/time) guarantees by the time this is
     * reachable.
     */
    suspend fun availableTimes(specialistId: String, dateKey: String): Result<List<TimeSlot>> {
        val repository = ManagerRepositories.availabilityRepository
            ?: return Result.failure(IllegalStateException("Availability is not ready yet — ManagerRepositories.initialize() has not completed"))
        val salon = ManagerRepositories.salonId
            ?: return Result.failure(IllegalStateException("Salon is not resolved yet — ManagerRepositories.initialize() has not completed"))
        val serviceId = _uiState.value.serviceId
            ?: return Result.failure(IllegalStateException("No service selected"))
        return repository.getAvailableSlots(
            salonId = salon,
            specialistId = specialistId,
            serviceId = serviceId,
            date = ManagerCalendarWeek.isoDateFor(dateKey),
        )
    }

    /**
     * Confirms the in-progress booking against the real backend
     * ([AppointmentRepository.createForCustomer], `POST
     * /api/v1/salons/{salonId}/bookings`). [ManagerBookingState.time] is
     * sent verbatim as `startTime` — it can only have been set via
     * [selectTime] with a value that came from a real [availableTimes]
     * result, so this call is only ever made with a real, backend-
     * confirmed-free slot, never a reconstructed or guessed one.
     */
    suspend fun confirm(): Result<Appointment> {
        val state = _uiState.value
        val customerId = state.customerId ?: return Result.failure(IllegalStateException("No customer selected"))
        val serviceId = state.serviceId ?: return Result.failure(IllegalStateException("No service selected"))
        val specialistId = state.specialistId ?: return Result.failure(IllegalStateException("No specialist selected"))
        val startTime = state.time ?: return Result.failure(IllegalStateException("No time selected"))

        _uiState.value = state.copy(isSubmitting = true, confirmError = null)
        val result = appointmentRepository.createForCustomer(
            customerId = customerId,
            serviceId = serviceId,
            specialistId = specialistId,
            startTime = startTime,
            notes = null,
        )
        _uiState.value = _uiState.value.copy(
            isSubmitting = false,
            createdAppointmentId = result.getOrNull()?.id,
            confirmError = result.exceptionOrNull()?.let(::confirmErrorMessage),
        )
        return result
    }
}

/**
 * Maps a real [confirm] failure to Persian, user-facing copy. Distinguishes
 * the specific backend error codes worth explaining differently (a customer
 * with no linked account can't be booked this way; the slot was taken by
 * someone else between selection and confirm) from a generic failure —
 * see [ai.rojan.designlab.data.remote.dto.ApiErrorDto.errorCode]'s doc
 * comment for where these codes come from.
 */
private fun confirmErrorMessage(error: Throwable): String {
    val apiError = (error as? BackendApiException)?.apiError
    return when (apiError?.errorCode) {
        "CUSTOMER_NOT_LINKED_TO_ACCOUNT" -> "این مشتری به حساب کاربری متصل نیست و امکان ثبت نوبت برای او وجود ندارد."
        "BOOKING_CONFLICT" -> "این بازه زمانی دیگر در دسترس نیست. لطفاً ساعت دیگری را انتخاب کنید."
        "SPECIALIST_NOT_FOUND", "SERVICE_NOT_FOUND", "CUSTOMER_NOT_FOUND" -> "اطلاعات انتخاب‌شده دیگر معتبر نیست. لطفاً دوباره تلاش کنید."
        else -> apiError?.message ?: "ثبت نوبت با خطا مواجه شد. لطفاً دوباره تلاش کنید."
    }
}
