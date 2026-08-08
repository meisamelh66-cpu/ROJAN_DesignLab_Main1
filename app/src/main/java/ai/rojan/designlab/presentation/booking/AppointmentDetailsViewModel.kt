package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class AppointmentDetailsData(
    val booking: Booking,
    val salonName: String?,
    val specialistName: String?,
    val serviceName: String?,
    val servicePrice: Double?,
)

/**
 * Loads everything `AppointmentDetailsScreen` needs from the real backend
 * for one booking. Unlike [BookingHistoryViewModel] (a *list* of bookings,
 * where a per-salon category+service scan would multiply across every
 * distinct salon on the page), this screen has exactly one known salon —
 * so resolving the service name/price via
 * `serviceCategoryRepository.getCategories(salonId)` then a per-category
 * services fan-out is bounded by that one salon's category count, the
 * same shape [ai.rojan.designlab.presentation.salon.SalonDetailsViewModel]
 * already uses for the identical reason.
 */
class AppointmentDetailsViewModel(
    private val appointmentId: String,
    private val bookingRepository: BookingRepository,
    private val salonRepository: SalonRepository,
    private val specialistRepository: SpecialistRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<AppointmentDetailsData>>(UiState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            val result = runCatching {
                val booking = bookingRepository.getBooking(appointmentId).getOrThrow()
                val salon = salonRepository.getSalon(booking.salonId).getOrNull()
                val specialist = specialistRepository.getSpecialist(booking.salonId, booking.specialistId).getOrNull()
                val categories = serviceCategoryRepository.getCategories(booking.salonId).getOrNull().orEmpty()
                val service = categories
                    .flatMap { category -> serviceRepository.getServices(booking.salonId, category.id).getOrNull().orEmpty() }
                    .find { it.id == booking.serviceId }

                AppointmentDetailsData(
                    booking = booking,
                    salonName = salon?.name,
                    specialistName = specialist?.displayName,
                    serviceName = service?.name,
                    servicePrice = service?.price,
                )
            }
            state = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(userMessageFor(it)) },
            )
        }
    }

    fun retry() = load()
}

class AppointmentDetailsViewModelFactory(
    private val appointmentId: String,
    private val bookingRepository: BookingRepository,
    private val salonRepository: SalonRepository,
    private val specialistRepository: SpecialistRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppointmentDetailsViewModel(
            appointmentId,
            bookingRepository,
            salonRepository,
            specialistRepository,
            serviceCategoryRepository,
            serviceRepository,
        ) as T
    }
}
