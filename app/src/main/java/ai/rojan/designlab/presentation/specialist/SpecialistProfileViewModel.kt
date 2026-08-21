package ai.rojan.designlab.presentation.specialist

import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
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

data class SpecialistProfileData(
    val specialist: Specialist,
    /** Every active service at the specialist's salon — the backend has no capability-to-service mapping, same disclosed simplification as before this milestone. */
    val services: List<Service>,
    /** Media System Evolution v2: this specialist's portfolio images. Enrichment, not a hard gate - a fetch failure degrades to empty rather than losing the whole profile. */
    val portfolio: List<String> = emptyList(),
)

/**
 * Loads [ai.rojan.designlab.screens.specialist.SpecialistProfileScreen]'s
 * data from the real backend. [salonId] is nullable — see
 * `RojanDestinations.SPECIALIST_PROFILE`'s doc comment for why: the
 * backend has no "get specialist by id alone" endpoint, only
 * `GET /api/v1/salons/{salonId}/specialists/{specialistId}`.
 */
class SpecialistProfileViewModel(
    private val salonId: String?,
    private val specialistId: String,
    private val specialistRepository: SpecialistRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<SpecialistProfileData>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            val resolvedSalonId = salonId
            if (resolvedSalonId == null) {
                state = UiState.Error("اطلاعات سالن این متخصص در دسترس نیست.")
                return@launch
            }

            val specialist = specialistRepository.getSpecialist(resolvedSalonId, specialistId).getOrElse {
                state = UiState.Error(userMessageFor(it))
                return@launch
            }

            val categories = serviceCategoryRepository.getCategories(resolvedSalonId).getOrElse {
                state = UiState.Error(userMessageFor(it))
                return@launch
            }

            val services = mutableListOf<Service>()
            for (category in categories) {
                val categoryServices = serviceRepository.getServices(resolvedSalonId, category.id).getOrElse {
                    state = UiState.Error(userMessageFor(it))
                    return@launch
                }
                services += categoryServices
            }

            val portfolio = specialistRepository.getPortfolio(resolvedSalonId, specialistId).getOrDefault(emptyList())

            state = UiState.Success(SpecialistProfileData(specialist, services, portfolio))
        }
    }

    fun retry() = load()
}
