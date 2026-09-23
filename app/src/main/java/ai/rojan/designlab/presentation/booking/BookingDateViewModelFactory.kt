package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.PublicSalonRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Manual factory, mirroring every other ViewModel factory in this app. */
class BookingDateViewModelFactory(
    private val salonId: String?,
    private val specialistId: String?,
    private val serviceId: String?,
    private val skipAutoSkip: Boolean,
    private val availabilityRepository: AvailabilityRepository,
    private val publicSalonRepository: PublicSalonRepository? = null,
    private val slug: String? = null,
    private val hasSession: () -> Boolean = { true },
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BookingDateViewModel(
            salonId = salonId,
            specialistId = specialistId,
            serviceId = serviceId,
            skipAutoSkip = skipAutoSkip,
            availabilityRepository = availabilityRepository,
            publicSalonRepository = publicSalonRepository,
            slug = slug,
            hasSession = hasSession,
        ) as T
    }
}
