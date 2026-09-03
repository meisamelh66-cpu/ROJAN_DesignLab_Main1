package ai.rojan.designlab.manager.presentation.calendar

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.manager.domain.appointment.Appointment
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * RBAC compatibility fix — Manager Android Pilot: owns the Confirm/Complete
 * actions for [ai.rojan.designlab.manager.screens.calendar.ManagerAppointmentDetailScreen],
 * which that screen's own doc comment previously said would need a
 * ViewModel once real mutation existed ("this screen currently has no
 * ViewModel at all... real mutation would need one, since isSubmitting/error
 * state can't live in a stateless @Composable"). Same
 * `isSubmitting`/error-mapping shape as
 * [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel],
 * for the same reason: real `Result`-returning repository calls need a place
 * to hold in-flight/error state that survives recomposition.
 *
 * [cancelBooking] (branch-integration reconciliation) follows
 * [confirmBooking]/[completeBooking]'s exact same shape —
 * [AppointmentRepository.cancel] is just as real and backend-persistent as
 * confirm/complete.
 *
 * [appointment] starts from [AppointmentRepository.getById] and is replaced
 * with the real, backend-returned row after a successful [confirmBooking]/
 * [completeBooking]/[cancelBooking] — not just optimistically flipped
 * locally — so the screen always reflects what the backend actually
 * persisted.
 */
class ManagerAppointmentDetailViewModel(
    private val appointmentRepository: AppointmentRepository,
    appointmentId: String,
) : ViewModel() {

    private val _appointment = MutableStateFlow(appointmentRepository.getById(appointmentId))
    val appointment: StateFlow<Appointment?> = _appointment.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Moves this booking from `PENDING` to `CONFIRMED` — only meaningful while [appointment] is `PENDING`; the screen gates the button on that, this does not re-check it. */
    fun confirmBooking() = mutate { appointmentRepository.confirm(it) }

    /** Moves this booking from `CONFIRMED` to `COMPLETED` — only meaningful while [appointment] is `CONFIRMED`; the screen gates the button on that, this does not re-check it. */
    fun completeBooking() = mutate { appointmentRepository.complete(it) }

    /** Moves this booking from `PENDING`/`CONFIRMED` to `CANCELLED` — same shape as [confirmBooking]/[completeBooking]. */
    fun cancelBooking() = mutate { appointmentRepository.cancel(it) }

    private fun mutate(call: suspend (id: String) -> Result<Appointment>) {
        val id = _appointment.value?.id ?: return
        _isSubmitting.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            call(id)
                .onSuccess { updated -> _appointment.value = updated }
                .onFailure { error -> _errorMessage.value = bookingActionErrorMessage(error) }
            _isSubmitting.value = false
        }
    }
}

/**
 * Maps a real [ManagerAppointmentDetailViewModel.confirmBooking]/[ManagerAppointmentDetailViewModel.completeBooking]/[ManagerAppointmentDetailViewModel.cancelBooking]
 * failure to Persian, user-facing copy — same idiom as
 * [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel]'s
 * own `confirmErrorMessage`, distinguishing the backend's real status-
 * transition/access error codes from a generic failure.
 */
private fun bookingActionErrorMessage(error: Throwable): String {
    val apiError = (error as? BackendApiException)?.apiError
    return when (apiError?.errorCode) {
        "INVALID_BOOKING_STATE" -> "این نوبت در وضعیتی نیست که بتوان این عملیات را روی آن انجام داد."
        "ACCESS_DENIED" -> "شما اجازه انجام این عملیات را ندارید."
        "BOOKING_NOT_FOUND" -> "این نوبت دیگر یافت نشد."
        else -> apiError?.message ?: "عملیات با خطا مواجه شد. لطفاً دوباره تلاش کنید."
    }
}
