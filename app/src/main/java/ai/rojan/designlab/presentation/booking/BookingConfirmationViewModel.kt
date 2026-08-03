package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.UUID

/** Real backend entities resolved for display on [ai.rojan.designlab.screens.bookingflow.BookingConfirmationScreen] and for recording the appointment once confirmed. */
data class BookingSummary(
    val salon: Salon? = null,
    val specialist: Specialist? = null,
    val service: Service? = null,
)

/**
 * Fires the real `POST /api/v1/bookings` call when the customer taps
 * "تایید نهایی رزرو" on [ai.rojan.designlab.screens.bookingflow.BookingConfirmationScreen].
 *
 * This call is genuinely best-effort, not a gate: the customer-ecosystem
 * "booking success" flow this screen leads into is local/demo state (no
 * backend equivalent — loyalty points, wallet cashback, reminders, etc.)
 * and has always completed unconditionally. Auth is frozen this milestone
 * (`di/BackendApiContainer.kt`'s doc comment) — this call *will* genuinely
 * 401 until a future milestone wires native Phone-OTP auth. Blocking the
 * existing, working local success flow on a network call known to be
 * dormant right now would be a real regression, not a fix, and there's no
 * user-facing way to resolve a 401 this milestone anyway. So: try the real
 * call, hand back the real booking id on success for
 * [ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel.bookAppointment]
 * to record (the join key real cancel later needs), and on any failure
 * hand back `null` — the local flow proceeds exactly as it did before this
 * milestone either way.
 */
class BookingConfirmationViewModel(
    private val bookingRepository: BookingRepository,
    private val salonRepository: SalonRepository,
    private val specialistRepository: SpecialistRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    var isSubmitting by mutableStateOf(false)
        private set

    var isLoadingSummary by mutableStateOf(false)
        private set

    var summary by mutableStateOf(BookingSummary())
        private set

    private var loadedForKey: Triple<String?, String?, String?>? = null

    /**
     * Resolves the real backend [Salon]/[Specialist]/[Service] for the ids
     * [BookingViewModel.state] is currently holding — this is what replaced
     * the old `CatalogEngine()` demo lookups, which broke once earlier
     * screens started carrying real backend ids instead of demo ones. A
     * service has no standalone "get by id" endpoint, so it's resolved the
     * same way [ai.rojan.designlab.presentation.service.ServiceDetailsViewModel]
     * already does: fan out over the salon's categories.
     */
    fun loadSummary(salonId: String?, specialistId: String?, serviceId: String?) {
        val key = Triple(salonId, specialistId, serviceId)
        if (key == loadedForKey) return
        loadedForKey = key
        if (salonId == null) {
            summary = BookingSummary()
            return
        }
        isLoadingSummary = true
        viewModelScope.launch {
            val loadedSalon = salonRepository.getSalon(salonId).getOrNull()
            val loadedSpecialist = specialistId?.let {
                specialistRepository.getSpecialist(salonId, it).getOrNull()
            }
            val loadedService = serviceId?.let { resolveService(salonId, it) }
            summary = BookingSummary(salon = loadedSalon, specialist = loadedSpecialist, service = loadedService)
            isLoadingSummary = false
        }
    }

    private suspend fun resolveService(salonId: String, serviceId: String): Service? {
        val categories = serviceCategoryRepository.getCategories(salonId).getOrNull() ?: return null
        for (category in categories) {
            val services = serviceRepository.getServices(salonId, category.id).getOrNull() ?: continue
            services.firstOrNull { it.id == serviceId }?.let { return it }
        }
        return null
    }

    fun confirmBooking(
        salonId: String?,
        serviceId: String?,
        specialistId: String?,
        dateKey: String?,
        time: String?,
        onResult: (backendBookingId: String?) -> Unit,
    ) {
        if (isSubmitting) return
        isSubmitting = true
        viewModelScope.launch {
            val backendBookingId = if (salonId != null && serviceId != null && specialistId != null && dateKey != null && time != null) {
                bookingRepository.createBooking(
                    salonId = salonId,
                    serviceId = serviceId,
                    specialistId = specialistId,
                    startTime = "${dateKey}T$time:00",
                    notes = null,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).getOrNull()?.id
            } else {
                null
            }
            isSubmitting = false
            onResult(backendBookingId)
        }
    }
}
