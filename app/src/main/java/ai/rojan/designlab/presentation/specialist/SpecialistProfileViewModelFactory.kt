package ai.rojan.designlab.presentation.specialist

import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class SpecialistProfileViewModelFactory(
    private val salonId: String?,
    private val specialistId: String,
    private val specialistRepository: SpecialistRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SpecialistProfileViewModel(
            salonId = salonId,
            specialistId = specialistId,
            specialistRepository = specialistRepository,
            serviceCategoryRepository = serviceCategoryRepository,
            serviceRepository = serviceRepository,
        ) as T
    }
}
