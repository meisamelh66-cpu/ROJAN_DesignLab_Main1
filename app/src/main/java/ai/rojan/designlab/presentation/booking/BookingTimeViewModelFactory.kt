package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.repository.AvailabilityRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class BookingTimeViewModelFactory(
    private val salonId: String?,
    private val specialistId: String?,
    private val serviceId: String?,
    private val date: String,
    private val availabilityRepository: AvailabilityRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BookingTimeViewModel(
            salonId = salonId,
            specialistId = specialistId,
            serviceId = serviceId,
            date = date,
            availabilityRepository = availabilityRepository,
        ) as T
    }
}
