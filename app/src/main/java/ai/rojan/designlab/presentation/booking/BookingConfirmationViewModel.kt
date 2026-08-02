package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.repository.BookingRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Fires the real `POST /api/v1/bookings` call when the customer taps
 * "تایید نهایی رزرو" on [ai.rojan.designlab.screens.bookingflow.BookingConfirmationScreen].
 *
 * This call is genuinely best-effort, not a gate: the customer-ecosystem
 * "booking success" flow this screen leads into is local/demo state (no
 * backend equivalent — loyalty points, wallet cashback, reminders, etc.)
 * and has always completed unconditionally. Auth is frozen this milestone
 * (`di/BackendApiContainer.kt`'s doc comment) — this call *will* genuinely
 * 401 until a future milestone wires native Phone-OTP auth. Blocking the
 * existing, working local success flow on a network call known to be
 * dormant right now would be a real regression, not a fix, and there's no
 * user-facing way to resolve a 401 this milestone anyway. So: try the real
 * call, hand back the real booking id on success for
 * [ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel.bookAppointment]
 * to record (the join key real cancel later needs), and on any failure
 * hand back `null` — the local flow proceeds exactly as it did before this
 * milestone either way.
 */
class BookingConfirmationViewModel(
    private val bookingRepository: BookingRepository,
) : ViewModel() {

    var isSubmitting by mutableStateOf(false)
        private set

    fun confirmBooking(
        salonId: String?,
        serviceId: String?,
        specialistId: String?,
        dateKey: String?,
        time: String?,
        onResult: (backendBookingId: String?) -> Unit,
    ) {
        if (isSubmitting) return
        isSubmitting = true
        viewModelScope.launch {
            val backendBookingId = if (salonId != null && serviceId != null && specialistId != null && dateKey != null && time != null) {
                bookingRepository.createBooking(
                    salonId = salonId,
                    serviceId = serviceId,
                    specialistId = specialistId,
                    startTime = "${dateKey}T$time:00",
                    notes = null,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).getOrNull()?.id
            } else {
                null
            }
            isSubmitting = false
            onResult(backendBookingId)
        }
    }
}
