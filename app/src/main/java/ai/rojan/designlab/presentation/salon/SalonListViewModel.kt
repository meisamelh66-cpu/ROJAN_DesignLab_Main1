package ai.rojan.designlab.presentation.salon

import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 50

/**
 * Browses active salons from the real backend (`GET /api/v1/salons`).
 * Replaces the previous `remember { CatalogEngine() }` -> `DemoSalonRepository`
 * read path for [ai.rojan.designlab.screens.booking.SalonListScreen] — the
 * first screen in this app to need a ViewModel for what used to be a
 * synchronous, always-available demo read.
 */
class SalonListViewModel(
    private val salonRepository: SalonRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<Salon>>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load(nameFilter: String? = null) {
        state = UiState.Loading
        viewModelScope.launch {
            salonRepository.browseSalons(page = 0, size = PAGE_SIZE, nameFilter = nameFilter)
                .onSuccess { paged ->
                    state = if (paged.content.isEmpty()) UiState.Empty else UiState.Success(paged.content)
                }
                .onFailure { error ->
                    state = UiState.Error(userMessageFor(error))
                }
        }
    }

    fun retry() = load()
}
