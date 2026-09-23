package ai.rojan.designlab.presentation.specialist

import ai.rojan.designlab.domain.repository.PublicSalonRepository
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
)

/**
 * Loads [ai.rojan.designlab.screens.specialist.SpecialistProfileScreen]'s
 * data from the real backend. [salonId] is nullable — see
 * `RojanDestinations.SPECIALIST_PROFILE`'s doc comment for why: the
 * backend has no "get specialist by id alone" endpoint, only
 * `GET /api/v1/salons/{salonId}/specialists/{specialistId}`.
 *
 * Guest Booking Flow fix: same guest/authenticated branch as
 * [ai.rojan.designlab.presentation.salon.SalonDetailsViewModel]. The public
 * API has no "one specialist by id" endpoint either — same shape as the
 * authenticated gap above — so the guest path fans out over
 * `getSpecialists(slug)` and picks the matching id, same pattern
 * [ai.rojan.designlab.presentation.service.ServiceDetailsViewModel] already
 * uses for services.
 */
class SpecialistProfileViewModel(
    private val salonId: String?,
    private val specialistId: String,
    private val specialistRepository: SpecialistRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
    private val publicSalonRepository: PublicSalonRepository? = null,
    private val slug: String? = null,
    private val hasSession: () -> Boolean = { true },
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

            val publicRepo = publicSalonRepository
            if (publicRepo != null && slug != null && !hasSession()) {
                loadPublic(publicRepo, slug, resolvedSalonId)
            } else {
                loadAuthenticated(resolvedSalonId)
            }
        }
    }

    private suspend fun loadAuthenticated(salonId: String) {
        val result = runCatching {
            val specialist = specialistRepository.getSpecialist(salonId, specialistId).getOrThrow()
            val categories = serviceCategoryRepository.getCategories(salonId).getOrThrow()
            val services = categories.flatMap { category ->
                serviceRepository.getServices(salonId, category.id).getOrThrow()
            }
            SpecialistProfileData(specialist, services)
        }
        state = result.fold(
            onSuccess = { UiState.Success(it) },
            onFailure = { UiState.Error(userMessageFor(it)) },
        )
    }

    /**
     * Guest path — the public API has no "one specialist by id" endpoint
     * (same gap as the authenticated side, just without a 404 to key off
     * of), so this fans out over [PublicSalonRepository.getSpecialists] and
     * matches locally — same "not found" shape as
     * [ai.rojan.designlab.presentation.service.ServiceDetailsViewModel].
     */
    private suspend fun loadPublic(publicRepo: PublicSalonRepository, slug: String, salonId: String) {
        val specialists = publicRepo.getSpecialists(slug).getOrElse {
            state = UiState.Error(userMessageFor(it))
            return
        }
        val specialistMatch = specialists.firstOrNull { it.id == specialistId }
        if (specialistMatch == null) {
            state = UiState.Error("این متخصص یافت نشد.")
            return
        }

        val result = runCatching {
            val categories = publicRepo.getCategories(slug).getOrThrow()
            val services = categories.flatMap { category ->
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
            SpecialistProfileData(
                specialist = Specialist(
                    id = specialistMatch.id,
                    salonId = salonId,
                    displayName = specialistMatch.displayName,
                    bio = specialistMatch.bio,
                    photoUrl = specialistMatch.photoUrl,
                ),
                services = services,
            )
        }
        state = result.fold(
            onSuccess = { UiState.Success(it) },
            onFailure = { UiState.Error(userMessageFor(it)) },
        )
    }

    fun retry() = load()
}
