package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed interface RescheduleUiState {
    data object Loading : RescheduleUiState
    data class Error(val message: String) : RescheduleUiState
    data class Ready(
        val dates: List<Pair<String, String>>,
        val selectedDate: String,
        val slots: UiState<List<TimeSlot>>,
        val selectedTime: String?,
        val isSubmitting: Boolean,
        val submitError: String? = null,
    ) : RescheduleUiState
}

/**
 * Backs `RescheduleAppointmentScreen` (Phase 2, C2 — un-gated: the backend
 * does expose `PUT /bookings/{id}/reschedule`, confirmed by reading
 * `ROJAN_Backend`'s `BookingController` directly; Phase 1 gated this
 * screen on the mistaken assumption that it didn't). Loads the existing
 * booking first (for its salon/specialist/service ids, needed by the
 * availability endpoint), then re-uses the same date-chips + time-grid
 * shape [ai.rojan.designlab.screens.bookingflow.BookingDateScreen]/
 * `BookingTimeScreen` already use for a *new* booking — this is the same
 * flow applied to an existing one, not a new mechanism.
 */
class RescheduleViewModel(
    private val appointmentId: String,
    private val bookingRepository: BookingRepository,
    private val availabilityRepository: AvailabilityRepository,
) : ViewModel() {

    var state by mutableStateOf<RescheduleUiState>(RescheduleUiState.Loading)
        private set

    private var salonId: String? = null
    private var specialistId: String? = null
    private var serviceId: String? = null

    init {
        loadBooking()
    }

    fun loadBooking() {
        state = RescheduleUiState.Loading
        viewModelScope.launch {
            bookingRepository.getBooking(appointmentId)
                .onSuccess { booking ->
                    salonId = booking.salonId
                    specialistId = booking.specialistId
                    serviceId = booking.serviceId
                    val dates = RollingBookingDates.next7Days()
                    val today = dates.first().first
                    state = RescheduleUiState.Ready(
                        dates = dates,
                        selectedDate = today,
                        slots = UiState.Loading,
                        selectedTime = null,
                        isSubmitting = false,
                    )
                    loadSlots(today)
                }
                .onFailure { error -> state = RescheduleUiState.Error(userMessageFor(error)) }
        }
    }

    fun selectDate(date: String) {
        val current = state as? RescheduleUiState.Ready ?: return
        if (current.selectedDate == date) return
        state = current.copy(selectedDate = date, slots = UiState.Loading, selectedTime = null)
        loadSlots(date)
    }

    private fun loadSlots(date: String) {
        val resolvedSalonId = salonId
        val resolvedSpecialistId = specialistId
        val resolvedServiceId = serviceId
        if (resolvedSalonId == null || resolvedSpecialistId == null || resolvedServiceId == null) return

        viewModelScope.launch {
            availabilityRepository.getAvailableSlots(resolvedSalonId, resolvedSpecialistId, resolvedServiceId, date)
                .onSuccess { slots ->
                    val current = state as? RescheduleUiState.Ready ?: return@onSuccess
                    if (current.selectedDate != date) return@onSuccess // stale response for a since-changed date
                    state = current.copy(slots = if (slots.isEmpty()) UiState.Empty else UiState.Success(slots))
                }
                .onFailure { error ->
                    val current = state as? RescheduleUiState.Ready ?: return@onFailure
                    if (current.selectedDate != date) return@onFailure
                    state = current.copy(slots = UiState.Error(userMessageFor(error)))
                }
        }
    }

    fun selectTime(time: String) {
        val current = state as? RescheduleUiState.Ready ?: return
        state = current.copy(selectedTime = time)
    }

    fun confirm(onRescheduled: () -> Unit) {
        val current = state as? RescheduleUiState.Ready ?: return
        val time = current.selectedTime ?: return
        state = current.copy(isSubmitting = true, submitError = null)
        viewModelScope.launch {
            bookingRepository.rescheduleBooking(appointmentId, "${current.selectedDate}T$time:00")
                .onSuccess { onRescheduled() }
                .onFailure { error ->
                    val latest = state as? RescheduleUiState.Ready ?: return@onFailure
                    state = latest.copy(isSubmitting = false, submitError = userMessageFor(error))
                }
        }
    }

    fun retry() = loadBooking()
}

class RescheduleViewModelFactory(
    private val appointmentId: String,
    private val bookingRepository: BookingRepository,
    private val availabilityRepository: AvailabilityRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RescheduleViewModel(appointmentId, bookingRepository, availabilityRepository) as T
    }
}
