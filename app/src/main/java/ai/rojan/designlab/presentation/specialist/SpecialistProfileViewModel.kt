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
    /**
     * Customer Specialist -> Services Integration: only the services this
     * specialist is actually eligible to perform, resolved from the real
     * `GET /specialists/{id}/services` assignment ids - the previous
     * "every active service at the salon" behavior was a stale assumption
     * (the backend capability-to-service mapping this class's own prior
     * doc comment claimed didn't exist is real, confirmed directly against
     * `SpecialistController.kt`). The eligibility decision itself is
     * Backend's alone; this list is never inferred or hard-coded locally -
     * it is exactly Backend's returned ids, cross-referenced against the
     * salon's already-fetched catalog only because no endpoint returns
     * full service objects scoped to a specialist directly.
     */
    val services: List<Service>,
    /**
     * True when Backend returned no assignment ids for this specialist -
     * its own documented meaning is "eligible for every service in the
     * salon", never "assigned to nothing". [services] is the full salon
     * catalog in that case, per that real business rule, not a fallback.
     */
    val isAssignedToEveryService: Boolean = false,
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

            val allServices = mutableListOf<Service>()
            for (category in categories) {
                val categoryServices = serviceRepository.getServices(resolvedSalonId, category.id).getOrElse {
                    state = UiState.Error(userMessageFor(it))
                    return@launch
                }
                allServices += categoryServices
            }

            // Real Backend relationship, not a hard-coded or inferred one - a fetch failure here
            // is a hard gate (same treatment as specialist/categories/services above), not a
            // silent fallback to "show everything", which would fabricate a relationship Backend
            // never actually confirmed.
            val assignedServiceIds = specialistRepository.getAssignedServiceIds(resolvedSalonId, specialistId).getOrElse {
                state = UiState.Error(userMessageFor(it))
                return@launch
            }
            val isAssignedToEveryService = assignedServiceIds.isEmpty()
            val eligibleServices = if (isAssignedToEveryService) {
                allServices
            } else {
                allServices.filter { it.id in assignedServiceIds }
            }

            val portfolio = specialistRepository.getPortfolio(resolvedSalonId, specialistId).getOrDefault(emptyList())

            state = UiState.Success(SpecialistProfileData(specialist, eligibleServices, isAssignedToEveryService, portfolio))
        }
    }

    fun retry() = load()
}
