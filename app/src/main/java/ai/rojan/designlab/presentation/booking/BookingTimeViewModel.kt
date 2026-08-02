package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Loads real bookable slots for
 * [ai.rojan.designlab.screens.bookingflow.BookingTimeScreen] from the
 * backend's computed-availability endpoint, replacing the demo
 * `BookingEngine.timeSlotsFor` call. [salonId]/[specialistId]/[serviceId]
 * are nullable because [BookingViewModel]'s state fields are — if any is
 * missing when this screen is reached (shouldn't happen via normal
 * navigation), this reports a clear error instead of guessing.
 */
class BookingTimeViewModel(
    private val salonId: String?,
    private val specialistId: String?,
    private val serviceId: String?,
    private val date: String,
    private val availabilityRepository: AvailabilityRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<TimeSlot>>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            val resolvedSalonId = salonId
            val resolvedSpecialistId = specialistId
            val resolvedServiceId = serviceId
            if (resolvedSalonId == null || resolvedSpecialistId == null || resolvedServiceId == null) {
                state = UiState.Error("اطلاعات لازم برای بارگذاری زمان‌های خالی در دسترس نیست.")
                return@launch
            }

            availabilityRepository.getAvailableSlots(resolvedSalonId, resolvedSpecialistId, resolvedServiceId, date)
                .onSuccess { slots ->
                    state = if (slots.isEmpty()) UiState.Empty else UiState.Success(slots)
                }
                .onFailure { error ->
                    state = UiState.Error(userMessageFor(error))
                }
        }
    }

    fun retry() = load()
}
