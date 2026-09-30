package ai.rojan.designlab.manager.presentation.booking

import ai.rojan.designlab.domain.repository.ActiveSalonContextRepository
import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.SalonCustomerRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.repository.AppointmentRepository
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
 *
 * **Master Integration Repair, Pass 4:** [appointmentRepositoryProvider]
 * replaces the previous, incorrect `BookingRepository` dependency - see
 * [ManagerBookingViewModel]'s own doc comment for why the customer
 * self-service repository was never the right one for this wizard. A
 * provider lambda, not a captured instance, matching this factory's own
 * call site (`{ ManagerRepositories.appointments }`) - the same Phase F2
 * Timing Fix shape used elsewhere in this app.
 */
class ManagerBookingViewModelFactory(
    private val salonRepository: SalonRepository,
    private val salonCustomerRepository: SalonCustomerRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
    private val specialistRepository: SpecialistRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val appointmentRepositoryProvider: () -> AppointmentRepository,
    private val activeSalonContextRepository: ActiveSalonContextRepository,
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
            appointmentRepositoryProvider = appointmentRepositoryProvider,
            activeSalonContextRepository = activeSalonContextRepository,
            savedStateHandle = extras.createSavedStateHandle(),
        ) as T
    }
}
