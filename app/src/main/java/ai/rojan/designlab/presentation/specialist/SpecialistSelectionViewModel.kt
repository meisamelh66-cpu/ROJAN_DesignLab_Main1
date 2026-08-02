package ai.rojan.designlab.presentation.specialist

import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Loads a salon's specialists for
 * [ai.rojan.designlab.screens.booking.SpecialistSelectionScreen] from the
 * real backend (`GET /api/v1/salons/{salonId}/specialists`).
 *
 * Before this milestone, this screen sorted specialists by earliest
 * available slot (walking every specialist x every day via the demo
 * `BookingEngine`), with rating as a tiebreaker. Both inputs are gone: the
 * backend `Specialist` has no rating, and computing "earliest available
 * slot per specialist" for real would mean one `available-slots` network
 * call per specialist per candidate date — expensive, and squarely
 * [ai.rojan.designlab.domain.repository.AvailabilityRepository]'s
 * territory (this milestone's Phase 5), not this one's. Specialists are
 * therefore shown in whatever order the backend returns them; genuine
 * earliest-availability sorting is deferred, not faked with a placeholder
 * ordering, and flagged here for whoever picks Phase 5 back up.
 */
class SpecialistSelectionViewModel(
    private val salonId: String,
    private val specialistRepository: SpecialistRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<Specialist>>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            specialistRepository.getSpecialists(salonId)
                .onSuccess { specialists ->
                    state = if (specialists.isEmpty()) UiState.Empty else UiState.Success(specialists)
                }
                .onFailure { error ->
                    state = UiState.Error(userMessageFor(error))
                }
        }
    }

    fun retry() = load()
}
