package ai.rojan.designlab.presentation.profile

import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/** Display-ready shape of the booking being rescheduled — resolved once when the screen loads, from the real backend booking, not local/demo state. */
data class RescheduleTarget(
    val salonId: String,
    val specialistId: String,
    val serviceId: String,
    val salonName: String,
    val serviceName: String,
    val specialistName: String,
)

/**
 * TEAM2-003 (Complete Booking API Contract). Backs
 * [ai.rojan.designlab.screens.profile.RescheduleAppointmentScreen] with
 * real backend data end to end, replacing its previous
 * [ai.rojan.designlab.domain.booking.BookingEngine]/
 * [ai.rojan.designlab.domain.catalog.CatalogEngine] demo logic:
 * - the booking itself comes from `GET /api/v1/bookings/{id}`
 *   ([BookingRepository.getBooking]) — the appointment id navigated in
 *   with is now always a real backend booking id (TEAM2-004), which the
 *   old `CustomerEcosystemViewModel`-backed lookup could never find;
 * - available slots for the newly-picked date come from the same real
 *   `available-slots` endpoint ([AvailabilityRepository]) the original
 *   booking flow already uses ([ai.rojan.designlab.presentation.booking.BookingTimeViewModel]);
 * - confirming calls the real `PUT /bookings/{id}/reschedule`
 *   ([BookingRepository.rescheduleBooking]).
 *
 * [onSuccess] in [confirmReschedule] fires only after that call actually
 * succeeds — same "no fake success" contract TEAM2-001 established for
 * booking confirmation.
 */
class RescheduleAppointmentViewModel(
    private val bookingId: String,
    private val bookingRepository: BookingRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val salonRepository: SalonRepository,
    private val specialistRepository: SpecialistRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    var targetState by mutableStateOf<UiState<RescheduleTarget>>(UiState.Loading)
        private set

    var selectedDateKey by mutableStateOf<String?>(null)
        private set

    var slotsState by mutableStateOf<UiState<List<TimeSlot>>>(UiState.Loading)
        private set

    var selectedTime by mutableStateOf<String?>(null)
        private set

    var isSubmitting by mutableStateOf(false)
        private set

    var submitError by mutableStateOf<String?>(null)
        private set

    init {
        loadTarget()
    }

    fun loadTarget() {
        targetState = UiState.Loading
        viewModelScope.launch {
            bookingRepository.getBooking(bookingId)
                .onSuccess { booking ->
                    val salonName = salonRepository.getSalon(booking.salonId).getOrNull()?.name ?: "—"
                    val specialistName = specialistRepository.getSpecialist(booking.salonId, booking.specialistId)
                        .getOrNull()?.displayName ?: "انتخاب خودکار"
                    val service = resolveService(booking.salonId, booking.serviceId)
                    targetState = UiState.Success(
                        RescheduleTarget(
                            salonId = booking.salonId,
                            specialistId = booking.specialistId,
                            serviceId = booking.serviceId,
                            salonName = salonName,
                            serviceName = service?.name ?: "—",
                            specialistName = specialistName,
                        ),
                    )
                }
                .onFailure { error ->
                    targetState = UiState.Error(userMessageFor(error))
                }
        }
    }

    fun retryLoadTarget() = loadTarget()

    fun selectDate(dateKey: String) {
        selectedDateKey = dateKey
        selectedTime = null
        loadSlots()
    }

    fun retryLoadSlots() = loadSlots()

    private fun loadSlots() {
        val target = (targetState as? UiState.Success)?.data ?: return
        val dateKey = selectedDateKey ?: return
        slotsState = UiState.Loading
        viewModelScope.launch {
            availabilityRepository.getAvailableSlots(target.salonId, target.specialistId, target.serviceId, dateKey)
                .onSuccess { slots ->
                    slotsState = if (slots.isEmpty()) UiState.Empty else UiState.Success(slots)
                }
                .onFailure { error ->
                    slotsState = UiState.Error(userMessageFor(error))
                }
        }
    }

    fun selectTime(time: String) {
        selectedTime = time
    }

    /** [onSuccess] is invoked if and only if the real reschedule call succeeded — never on a missing selection or a failure. */
    fun confirmReschedule(onSuccess: () -> Unit) {
        if (isSubmitting) return
        submitError = null

        val dateKey = selectedDateKey
        val time = selectedTime
        if (dateKey == null || time == null) {
            submitError = "لطفاً تاریخ و ساعت جدید را انتخاب کنید."
            return
        }

        isSubmitting = true
        viewModelScope.launch {
            bookingRepository.rescheduleBooking(bookingId, "${dateKey}T$time:00")
                .onSuccess {
                    isSubmitting = false
                    onSuccess()
                }
                .onFailure { error ->
                    isSubmitting = false
                    submitError = userMessageFor(error)
                }
        }
    }

    /** A service has no standalone "get by id" endpoint — same fan-out-over-categories resolution [ai.rojan.designlab.presentation.booking.BookingConfirmationViewModel.resolveService]/[AppointmentsViewModel.resolveService] already use. */
    private suspend fun resolveService(salonId: String, serviceId: String): Service? {
        val categories = serviceCategoryRepository.getCategories(salonId).getOrNull() ?: return null
        for (category in categories) {
            val services = serviceRepository.getServices(salonId, category.id).getOrNull() ?: continue
            services.firstOrNull { it.id == serviceId }?.let { return it }
        }
        return null
    }
}
