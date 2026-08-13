package ai.rojan.designlab.reception.presentation.dashboard

import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import ai.rojan.designlab.reception.domain.repository.ReceptionBookingRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads the salon's real booking list (`GET /api/v1/salons/{salonId}/bookings`)
 * — owner-only today, so this will genuinely surface an authorization
 * [UiState.Error] until `ROJAN_System1_Backend_Decision_v2.md` §4 item 6
 * ships. That is the correct, honest behavior — this class does not fall
 * back to any mock/fake list on failure.
 *
 * [confirmBooking]/[completeBooking] (review-fixes phase, fix 3) call the
 * real, existing `PATCH /bookings/{id}/confirm|complete` endpoints via the
 * flavor-agnostic [genericBookingRepository] — same "will legitimately
 * fail authorization until §4 item 6 ships" status as [bookingRepository].
 */
class ReceptionDashboardViewModel(
    private val salonId: String,
    private val bookingRepository: ReceptionBookingRepository,
    private val genericBookingRepository: BookingRepository,
) : ViewModel() {

    private val _bookings = MutableStateFlow<UiState<List<Booking>>>(UiState.Loading)
    val bookings: StateFlow<UiState<List<Booking>>> = _bookings.asStateFlow()

    private val _processingBookingId = MutableStateFlow<String?>(null)
    val processingBookingId: StateFlow<String?> = _processingBookingId.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _bookings.value = UiState.Loading
        viewModelScope.launch {
            bookingRepository.listBookings(salonId)
                .onSuccess { result ->
                    _bookings.value = if (result.content.isEmpty()) UiState.Empty else UiState.Success(result.content)
                }
                .onFailure { error -> _bookings.value = UiState.Error(userMessageFor(error)) }
        }
    }

    /** `PENDING -> CONFIRMED`. Re-[refresh]es the list on success so the row's status reflects the real, new state rather than an optimistic guess. */
    fun confirmBooking(bookingId: String) = performAction(bookingId) { genericBookingRepository.confirmBooking(bookingId) }

    /** `CONFIRMED -> COMPLETED`. Same reasoning as [confirmBooking]. */
    fun completeBooking(bookingId: String) = performAction(bookingId) { genericBookingRepository.completeBooking(bookingId) }

    private fun performAction(bookingId: String, action: suspend () -> Result<Booking>) {
        _actionError.value = null
        _processingBookingId.value = bookingId
        viewModelScope.launch {
            action()
                .onSuccess { refresh() }
                .onFailure { error -> _actionError.value = userMessageFor(error) }
            _processingBookingId.value = null
        }
    }
}
