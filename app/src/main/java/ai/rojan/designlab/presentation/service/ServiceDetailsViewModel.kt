package ai.rojan.designlab.presentation.service

import ai.rojan.designlab.domain.repository.PublicSalonRepository
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
 *
 * Guest Booking Flow fix: mirrors [ai.rojan.designlab.presentation.salon.SalonDetailsViewModel]'s
 * guest/authenticated branch exactly — `GET /api/v1/salons/{salonId}/categories`
 * and its `/services` sibling require auth unconditionally, so a guest
 * always 401ed here before this fix, one tap after Salon Detail (which
 * already worked for guests). [slug]/[publicSalonRepository]/[hasSession]
 * default to the pre-existing always-authenticated behavior for any call
 * site that doesn't wire guest browsing.
 */
class ServiceDetailsViewModel(
    private val salonId: String?,
    private val serviceId: String,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
    private val publicSalonRepository: PublicSalonRepository? = null,
    private val slug: String? = null,
    private val hasSession: () -> Boolean = { true },
) : ViewModel() {

    var state by mutableStateOf<UiState<Service>>(UiState.Loading)
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

            val publicRepo = publicSalonRepository
            val servicesResult = if (publicRepo != null && slug != null && !hasSession()) {
                loadPublicServices(publicRepo, slug, resolvedSalonId)
            } else {
                loadAuthenticatedServices(resolvedSalonId)
            }

            val services = servicesResult.getOrElse {
                state = UiState.Error(userMessageFor(it))
                return@launch
            }

            val match = services.firstOrNull { it.id == serviceId }
            state = if (match != null) UiState.Success(match) else UiState.Error("این خدمت یافت نشد.")
        }
    }

    private suspend fun loadAuthenticatedServices(salonId: String): Result<List<Service>> = runCatching {
        val categories = serviceCategoryRepository.getCategories(salonId).getOrThrow()
        categories.flatMap { category -> serviceRepository.getServices(salonId, category.id).getOrThrow() }
    }

    private suspend fun loadPublicServices(
        publicRepo: PublicSalonRepository,
        slug: String,
        salonId: String,
    ): Result<List<Service>> = runCatching {
        val categories = publicRepo.getCategories(slug).getOrThrow()
        categories.flatMap { category ->
            publicRepo.getServices(slug, category.id).getOrThrow().map { service ->
                Service(
                    id = service.id,
                    salonId = salonId,
                    categoryId = service.categoryId,
                    name = service.name,
                    description = service.description,
                    durationMinutes = service.durationMinutes,
                    price = service.price,
                )
            }
        }
    }

    fun retry() = load()
}
