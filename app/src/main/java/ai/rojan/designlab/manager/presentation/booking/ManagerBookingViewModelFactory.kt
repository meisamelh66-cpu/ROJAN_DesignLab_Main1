package ai.rojan.designlab.manager.presentation.booking

import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.SalonCustomerRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Manual factory for [ManagerBookingViewModel], mirroring every other
 * ViewModel factory in this app.
 *
 * Manager Booking Creation Integrity follow-up: wires the real backend
 * repositories (from [ai.rojan.designlab.di.BackendApiContainer]) instead
 * of `manager.data.ManagerRepositories`' in-memory ones.
 */
class ManagerBookingViewModelFactory(
    private val salonRepository: SalonRepository,
    private val salonCustomerRepository: SalonCustomerRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
    private val specialistRepository: SpecialistRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val bookingRepository: BookingRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ManagerBookingViewModel(
            salonRepository,
            salonCustomerRepository,
            serviceCategoryRepository,
            serviceRepository,
            specialistRepository,
            availabilityRepository,
            bookingRepository,
        ) as T
    }
}
