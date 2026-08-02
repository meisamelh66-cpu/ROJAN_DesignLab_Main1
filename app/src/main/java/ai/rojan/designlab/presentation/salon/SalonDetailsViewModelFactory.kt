package ai.rojan.designlab.presentation.salon

import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class SalonDetailsViewModelFactory(
    private val salonId: String,
    private val salonRepository: SalonRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
    private val specialistRepository: SpecialistRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SalonDetailsViewModel(
            salonId = salonId,
            salonRepository = salonRepository,
            serviceCategoryRepository = serviceCategoryRepository,
            serviceRepository = serviceRepository,
            specialistRepository = specialistRepository,
        ) as T
    }
}
