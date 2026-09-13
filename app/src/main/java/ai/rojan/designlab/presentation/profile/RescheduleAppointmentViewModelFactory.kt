package ai.rojan.designlab.presentation.profile

import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app (e.g. `AppointmentsViewModelFactory`). */
class RescheduleAppointmentViewModelFactory(
    private val bookingId: String,
    private val bookingRepository: BookingRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val salonRepository: SalonRepository,
    private val specialistRepository: SpecialistRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RescheduleAppointmentViewModel(
            bookingId,
            bookingRepository,
            availabilityRepository,
            salonRepository,
            specialistRepository,
            serviceCategoryRepository,
            serviceRepository,
        ) as T
    }
}
