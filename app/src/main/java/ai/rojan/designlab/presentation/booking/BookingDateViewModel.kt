package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Real replacement for [ai.rojan.designlab.screens.bookingflow.BookingDateScreen]'s
 * "auto-preselect the first available day when today is full" check.
 * Previously walked the demo [ai.rojan.designlab.domain.booking.BookingEngine]
 * in memory; now calls the real `available-slots` endpoint once for today,
 * and — only if today is empty — once per subsequent candidate day
 * (stopping at the first hit), via [AvailabilityRepository]. Bounded to at
 * most 7 real network calls, same "no bulk endpoint exists" reasoning as
 * [ai.rojan.designlab.presentation.specialist.SpecialistSelectionViewModel]'s
 * doc comment.
 *
 * [salonId]/[specialistId]/[serviceId] are nullable because
 * [ai.rojan.designlab.presentation.booking.BookingViewModel]'s state
 * fields are nullable; if any is missing when this screen is reached
 * (shouldn't happen via normal navigation), this reports a clear error
 * instead of guessing. [skipAutoSkip] mirrors the demo version's "only
 * auto-advance on a genuinely fresh entry" guard — evaluated once, at
 * construction, from whether a date was already recorded for this booking
 * session.
 */
class BookingDateViewModel(
    private val salonId: String?,
    private val specialistId: String?,
    private val serviceId: String?,
    private val skipAutoSkip: Boolean,
    private val availabilityRepository: AvailabilityRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<Unit>>(UiState.Loading)
        private set

    var autoSelectedDate by mutableStateOf<String?>(null)
        private set

    init {
        checkAvailability()
    }

    fun checkAvailability() {
        state = UiState.Loading
        autoSelectedDate = null
        viewModelScope.launch {
            if (skipAutoSkip) {
                state = UiState.Success(Unit)
                return@launch
            }

            val resolvedSalonId = salonId
            val resolvedSpecialistId = specialistId
            val resolvedServiceId = serviceId
            if (resolvedSalonId == null || resolvedSpecialistId == null || resolvedServiceId == null) {
                state = UiState.Error("اطلاعات لازم برای بررسی زمان‌های خالی در دسترس نیست.")
                return@launch
            }

            val dates = RollingBookingDates.next7Days()
            val today = dates.first()
            val todaySlots = availabilityRepository
                .getAvailableSlots(resolvedSalonId, resolvedSpecialistId, resolvedServiceId, today.first)
                .getOrElse {
                    state = UiState.Error(userMessageFor(it))
                    return@launch
                }

            if (todaySlots.isEmpty()) {
                for (dateEntry in dates.drop(1)) {
                    val slots = availabilityRepository
                        .getAvailableSlots(resolvedSalonId, resolvedSpecialistId, resolvedServiceId, dateEntry.first)
                        .getOrElse {
                            state = UiState.Error(userMessageFor(it))
                            return@launch
                        }
                    if (slots.isNotEmpty()) {
                        autoSelectedDate = dateEntry.first
                        break
                    }
                }
            }

            state = UiState.Success(Unit)
        }
    }

    fun retry() = checkAvailability()
}
