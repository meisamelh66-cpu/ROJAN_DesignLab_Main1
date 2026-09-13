package ai.rojan.designlab.manager.presentation.dashboard

import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app (e.g. `AppointmentsViewModelFactory`). */
class ManagerDashboardViewModelFactory(
    private val salonRepository: SalonRepository,
    private val bookingRepository: BookingRepository,
    private val specialistRepository: SpecialistRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerDashboardViewModel(
            salonRepository,
            bookingRepository,
            specialistRepository,
            serviceCategoryRepository,
            serviceRepository,
        ) as T
    }
}
