package ai.rojan.designlab.presentation.salon

import ai.rojan.designlab.domain.repository.PublicSalon
import ai.rojan.designlab.domain.repository.PublicSalonRepository
import ai.rojan.designlab.domain.repository.PublicServiceCategory
import ai.rojan.designlab.domain.repository.PublicSpecialist
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class PublicSalonServiceGroup(val category: PublicServiceCategory, val services: List<ai.rojan.designlab.domain.repository.PublicService>)

data class PublicSalonData(
    val salon: PublicSalon,
    val serviceGroups: List<PublicSalonServiceGroup>,
    val specialists: List<PublicSpecialist>,
)

/**
 * ROJAN AI Customer Journey Phase 2 (P0-2): the QR-scan/deep-link entry
 * point's screen-level ViewModel — the counterpart [PublicSalonRepository]'s
 * own doc comment flagged as "a separate, later decision" when the data
 * layer was first built. Deliberately read-only/preview-only: no
 * booking/follow/favorite action lives here, since all of those need a real
 * customer session ([PublicSalonRepository] itself never carries one) —
 * this screen's only "action" is routing to [ai.rojan.designlab.navigation.RojanDestinations.AUTH].
 */
class PublicSalonViewModel(
    private val slug: String,
    private val publicSalonRepository: PublicSalonRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<PublicSalonData>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            val result = runCatching {
                val salon = publicSalonRepository.getSalon(slug).getOrThrow()
                val categories = publicSalonRepository.getCategories(slug).getOrThrow()
                val serviceGroups = categories.map { category ->
                    PublicSalonServiceGroup(category, publicSalonRepository.getServices(slug, category.id).getOrThrow())
                }
                val specialists = publicSalonRepository.getSpecialists(slug).getOrThrow()
                PublicSalonData(salon, serviceGroups, specialists)
            }
            state = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(userMessageFor(it)) },
            )
        }
    }

    fun retry() = load()
}
