package ai.rojan.designlab.manager.presentation.calendar

import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.appointment.AppointmentStatus
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manager Booking Actions (Android Pilot P0 gap) — confirm/cancel/complete
 * for [ai.rojan.designlab.manager.screens.calendar.ManagerAppointmentDetailScreen].
 * Calls the real, existing `PATCH .../bookings/{id}/confirm|cancel|complete`
 * endpoints via the flavor-agnostic [genericBookingRepository]
 * ([BookingRepository]) — the exact same pattern
 * [ai.rojan.designlab.reception.presentation.dashboard.ReceptionDashboardViewModel]
 * already uses for the identical problem, not a new mechanism. No new
 * repository: [appointmentRepository] stays
 * [ai.rojan.designlab.manager.data.ManagerRepositories.appointments], and a
 * successful action reflects the fresh status into it via its own
 * [AppointmentRepository.updateStatus] (previously dead code, now this
 * class's real call site) rather than a full
 * [ai.rojan.designlab.manager.data.BackendAppointmentRepository.sync].
 */
class ManagerAppointmentDetailViewModel(
    private val appointmentId: String,
    private val appointmentRepository: AppointmentRepository,
    private val genericBookingRepository: BookingRepository,
) : ViewModel() {

    private val _appointment = MutableStateFlow(appointmentRepository.getById(appointmentId))
    val appointment: StateFlow<Appointment?> = _appointment.asStateFlow()

    var isSubmitting by mutableStateOf(false)
        private set

    var actionError by mutableStateOf<String?>(null)
        private set

    fun confirm() = performAction { genericBookingRepository.confirmBooking(appointmentId) }

    fun cancel() = performAction { genericBookingRepository.cancelBooking(appointmentId) }

    fun complete() = performAction { genericBookingRepository.completeBooking(appointmentId) }

    private fun performAction(action: suspend () -> Result<Booking>) {
        actionError = null
        isSubmitting = true
        viewModelScope.launch {
            action()
                .onSuccess { booking ->
                    _appointment.value = appointmentRepository.updateStatus(appointmentId, booking.status.toAppointmentStatus())
                }
                .onFailure { error -> actionError = userMessageFor(error) }
            isSubmitting = false
        }
    }
}

private fun BookingStatus.toAppointmentStatus(): AppointmentStatus = when (this) {
    BookingStatus.PENDING -> AppointmentStatus.PENDING
    BookingStatus.CONFIRMED -> AppointmentStatus.CONFIRMED
    BookingStatus.CANCELLED -> AppointmentStatus.CANCELLED
    BookingStatus.COMPLETED -> AppointmentStatus.COMPLETED
}
