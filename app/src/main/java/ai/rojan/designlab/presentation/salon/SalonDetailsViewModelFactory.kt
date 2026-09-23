package ai.rojan.designlab.presentation.salon

import ai.rojan.designlab.domain.repository.PublicSalonRepository
import ai.rojan.designlab.domain.repository.SalonGalleryRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.domain.repository.WorkingHoursRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class SalonDetailsViewModelFactory(
    private val salonId: String,
    private val salonRepository: SalonRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
    private val specialistRepository: SpecialistRepository,
    private val workingHoursRepository: WorkingHoursRepository,
    private val salonGalleryRepository: SalonGalleryRepository,
    /** Guest Salon Detail fix — see [SalonDetailsViewModel]'s own doc comment. */
    private val slug: String? = null,
    private val publicSalonRepository: PublicSalonRepository? = null,
    private val hasSession: () -> Boolean = { true },
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SalonDetailsViewModel(
            salonId = salonId,
            salonRepository = salonRepository,
            serviceCategoryRepository = serviceCategoryRepository,
            serviceRepository = serviceRepository,
            specialistRepository = specialistRepository,
            workingHoursRepository = workingHoursRepository,
            salonGalleryRepository = salonGalleryRepository,
            slug = slug,
            publicSalonRepository = publicSalonRepository,
            hasSession = hasSession,
        ) as T
    }
}
