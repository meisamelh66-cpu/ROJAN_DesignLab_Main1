package ai.rojan.designlab.presentation.service

import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class ServiceDetailsData(
    val service: Service,
    /** Media System Evolution v2: this service's images. Enrichment, not a hard gate - a fetch failure degrades to empty rather than losing the whole detail screen. */
    val images: List<String> = emptyList(),
)

/**
 * Loads a single [Service] for [ai.rojan.designlab.screens.service.ServiceDetailsScreen].
 * There is no salon-wide "all services" endpoint, and no "get service by id
 * alone" endpoint either (`ROJAN_Backend/API_CONTRACT.md` scopes a service
 * GET under both its salon and its category) — so this fans out over the
 * salon's categories, same as [ai.rojan.designlab.presentation.salon.SalonDetailsViewModel],
 * and picks out the matching id.
 *
 * [salonId] is nullable because the app's navigation graph doesn't carry it
 * on every path that can reach this screen (e.g. the "rebook" shortcut from
 * a past appointment) — see the call sites in `RojanNavGraph.kt`. When
 * null, this reports a clear error rather than guessing or crashing.
 */
class ServiceDetailsViewModel(
    private val salonId: String?,
    private val serviceId: String,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<ServiceDetailsData>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            val resolvedSalonId = salonId
            if (resolvedSalonId == null) {
                state = UiState.Error("اطلاعات سالن این خدمت در دسترس نیست.")
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

            val match = services.firstOrNull { it.id == serviceId }
            if (match == null) {
                state = UiState.Error("این خدمت یافت نشد.")
                return@launch
            }
            val images = serviceRepository.getImages(resolvedSalonId, match.id).getOrDefault(emptyList())
            state = UiState.Success(ServiceDetailsData(match, images))
        }
    }

    fun retry() = load()
}
