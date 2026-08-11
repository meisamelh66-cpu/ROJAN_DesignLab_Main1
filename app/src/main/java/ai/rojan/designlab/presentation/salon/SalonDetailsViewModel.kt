package ai.rojan.designlab.presentation.salon

import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.SalonWorkingHours
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategory
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.domain.repository.WorkingHoursRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class SalonDetailsData(
    val salon: Salon,
    val categories: List<ServiceCategory>,
    val services: List<Service>,
    val specialists: List<Specialist>,
    val workingHours: List<SalonWorkingHours>,
)

/**
 * Loads everything [ai.rojan.designlab.screens.salon.SalonDetailsScreen]
 * needs from the real backend. There is no salon-wide "list every service"
 * endpoint (`ROJAN_Backend/API_CONTRACT.md` only exposes services scoped to
 * a category), so services are fetched by fanning out one call per category
 * and flattening — the same shape the screen already needed (grouped
 * services), just sourced from N+1 calls instead of one.
 */
class SalonDetailsViewModel(
    private val salonId: String,
    private val salonRepository: SalonRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
    private val specialistRepository: SpecialistRepository,
    private val workingHoursRepository: WorkingHoursRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<SalonDetailsData>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            val result = runCatching {
                val salon = salonRepository.getSalon(salonId).getOrThrow()
                val categories = serviceCategoryRepository.getCategories(salonId).getOrThrow()
                val services = categories.flatMap { category ->
                    serviceRepository.getServices(salonId, category.id).getOrThrow()
                }
                val specialists = specialistRepository.getSpecialists(salonId).getOrThrow()
                // Enrichment, not a hard gate (same principle BookingStepResolver already
                // applies to Salon): a working-hours fetch failure shouldn't take down the
                // whole salon page, so it degrades to an empty list instead of getOrThrow().
                val workingHours = workingHoursRepository.getWorkingHours(salonId).getOrDefault(emptyList())
                SalonDetailsData(salon, categories, services, specialists, workingHours)
            }
            state = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(userMessageFor(it)) },
            )
        }
    }

    fun retry() = load()
}
