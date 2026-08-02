package ai.rojan.designlab.presentation.service

import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class ServiceDetailsViewModelFactory(
    private val salonId: String?,
    private val serviceId: String,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ServiceDetailsViewModel(
            salonId = salonId,
            serviceId = serviceId,
            serviceCategoryRepository = serviceCategoryRepository,
            serviceRepository = serviceRepository,
        ) as T
    }
}
