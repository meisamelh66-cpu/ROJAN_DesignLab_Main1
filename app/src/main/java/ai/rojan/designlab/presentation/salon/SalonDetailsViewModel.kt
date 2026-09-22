package ai.rojan.designlab.presentation.salon

import ai.rojan.designlab.domain.repository.PublicSalonRepository
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
    // Guest Salon Detail fix: mirrors SalonListViewModel's established
    // guest/authenticated branch (see that class's own doc comment,
    // "Guest Explore fix"). GET /api/v1/salons/{salonId} (and its
    // categories/services/specialists/working-hours siblings) require auth
    // unconditionally (ROJAN_Backend SecurityConfig: anyRequest().authenticated(),
    // only /api/v1/public/** is allow-listed) — a guest always 401s on the
    // authenticated path. [slug] is only ever non-null when this salon was
    // reached from a guest-visible list (PublicSalonRepository-sourced,
    // which always carries a real slug); [publicSalonRepository]/[hasSession]
    // default to the pre-existing always-authenticated behavior for any call
    // site that doesn't wire guest browsing, so nothing changes for them.
    private val slug: String? = null,
    private val publicSalonRepository: PublicSalonRepository? = null,
    private val hasSession: () -> Boolean = { true },
) : ViewModel() {

    var state by mutableStateOf<UiState<SalonDetailsData>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            val publicRepo = publicSalonRepository
            val result = if (publicRepo != null && slug != null && !hasSession()) {
                loadPublic(publicRepo, slug)
            } else {
                loadAuthenticated()
            }
            state = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(userMessageFor(it)) },
            )
        }
    }

    private suspend fun loadAuthenticated(): Result<SalonDetailsData> = runCatching {
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

    /**
     * Guest path — the unauthenticated `/api/v1/public/salons/{slug}/...`
     * family. No public working-hours endpoint exists, so that field
     * always degrades to an empty list here (same "enrichment, not a hard
     * gate" principle as the authenticated path's own working-hours
     * fetch). [PublicSalon]/[PublicServiceCategory]/[PublicService]/
     * [PublicSpecialist] map onto the same [SalonDetailsData] shape the
     * screen already renders, so no UI code needs to change.
     */
    private suspend fun loadPublic(
        publicRepo: PublicSalonRepository,
        slug: String,
    ): Result<SalonDetailsData> = runCatching {
        val publicSalon = publicRepo.getSalon(slug).getOrThrow()
        val salon = Salon(
            id = publicSalon.id,
            name = publicSalon.name,
            description = publicSalon.description,
            phone = publicSalon.phone,
            email = null,
            address = publicSalon.address,
            logoUrl = publicSalon.logoUrl,
            latitude = publicSalon.latitude,
            longitude = publicSalon.longitude,
            slug = slug,
        )
        val publicCategories = publicRepo.getCategories(slug).getOrThrow()
        val categories = publicCategories.map { category ->
            ServiceCategory(id = category.id, salonId = salon.id, name = category.name, description = category.description)
        }
        val services = publicCategories.flatMap { category ->
            publicRepo.getServices(slug, category.id).getOrThrow().map { service ->
                Service(
                    id = service.id,
                    salonId = salon.id,
                    categoryId = service.categoryId,
                    name = service.name,
                    description = service.description,
                    durationMinutes = service.durationMinutes,
                    price = service.price,
                )
            }
        }
        val specialists = publicRepo.getSpecialists(slug).getOrThrow().map { specialist ->
            Specialist(
                id = specialist.id,
                salonId = salon.id,
                displayName = specialist.displayName,
                bio = specialist.bio,
                photoUrl = specialist.photoUrl,
            )
        }
        SalonDetailsData(salon, categories, services, specialists, workingHours = emptyList())
    }

    fun retry() = load()
}
