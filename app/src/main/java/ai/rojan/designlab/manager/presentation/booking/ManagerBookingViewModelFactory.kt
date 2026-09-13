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
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Manual factory for [ManagerBookingViewModel], mirroring every other
 * ViewModel factory in this app.
 *
 * Manager Booking Creation Integrity follow-up: wires the real backend
 * repositories (from [ai.rojan.designlab.di.BackendApiContainer]) instead
 * of `manager.data.ManagerRepositories`' in-memory ones.
 *
 * **5B6-1 (preserved from Handoff):** overrides the `CreationExtras`
 * overload (not just the plain `Class<T>` one) so [createSavedStateHandle]
 * can source a real [androidx.lifecycle.SavedStateHandle] from the extras
 * supplied at the call site (`managerBookingViewModelFor` in
 * `ManagerNavGraph.kt`, which passes `parentEntry.defaultViewModelCreationExtras`)
 * — the API Navigation itself restores through, so the wizard's five
 * selections survive process death.
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
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return ManagerBookingViewModel(
            salonRepository = salonRepository,
            salonCustomerRepository = salonCustomerRepository,
            serviceCategoryRepository = serviceCategoryRepository,
            serviceRepository = serviceRepository,
            specialistRepository = specialistRepository,
            availabilityRepository = availabilityRepository,
            bookingRepository = bookingRepository,
            savedStateHandle = extras.createSavedStateHandle(),
        ) as T
    }
}
