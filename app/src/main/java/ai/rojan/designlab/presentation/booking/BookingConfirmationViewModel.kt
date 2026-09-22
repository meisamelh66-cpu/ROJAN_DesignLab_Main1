package ai.rojan.designlab.presentation.booking

import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.Salon
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.presentation.common.userMessageFor
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
 * Phase 2 (C4): this doc comment previously claimed auth was "frozen" and
 * this call "*will* genuinely 401" — both stale. Real email/password auth
 * (`AuthScreen`/`AuthViewModel`) has been fully wired since the Android
 * <-> Backend Full Integration milestone, and `createBooking` goes
 * through the same authenticated `Retrofit` instance every other
 * confirmed-working call does. It was also silently swallowing failures
 * (`getOrNull()`, discarding the error) and letting the screen navigate
 * to "Booking Success" regardless — a real bug once the demo ecosystem
 * fallback this was written against (deleted in Phase 1) stopped existing
 * to fall back to. [confirmBooking] now only calls [onSuccess] on a
 * genuine success, and exposes [submitError] on failure so the screen can
 * show it and let the customer retry, instead of claiming success either
 * way.
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

    var submitError by mutableStateOf<String?>(null)
        private set

    /**
     * Set when [loadSummary] genuinely fails to reach the backend (network/
     * server error), as opposed to a field simply not resolving (e.g. no
     * specialist chosen yet). Previously this class used `getOrNull()`
     * everywhere and threw the error away, so a load failure rendered as
     * permanent "—" placeholders with no way to tell the customer or let
     * them retry.
     */
    var summaryError by mutableStateOf<String?>(null)
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
        summaryError = null
        viewModelScope.launch {
            val salonResult = salonRepository.getSalon(salonId)
            val specialistResult = specialistId?.let { specialistRepository.getSpecialist(salonId, it) }
            val serviceResult = serviceId?.let { resolveService(salonId, it) }
            summary = BookingSummary(
                salon = salonResult.getOrNull(),
                specialist = specialistResult?.getOrNull(),
                service = serviceResult?.getOrNull(),
            )
            summaryError = (salonResult.exceptionOrNull()
                ?: specialistResult?.exceptionOrNull()
                ?: serviceResult?.exceptionOrNull())
                ?.let { userMessageFor(it) }
            isLoadingSummary = false
        }
    }

    /** Retries the most recent [loadSummary] request after a failure. */
    fun retryLoadSummary(salonId: String?, specialistId: String?, serviceId: String?) {
        loadedForKey = null
        loadSummary(salonId, specialistId, serviceId)
    }

    private suspend fun resolveService(salonId: String, serviceId: String): Result<Service?> {
        val categoriesResult = serviceCategoryRepository.getCategories(salonId)
        val categories = categoriesResult.getOrNull()
            ?: return categoriesResult.exceptionOrNull()?.let { Result.failure(it) } ?: Result.success(null)
        for (category in categories) {
            val services = serviceRepository.getServices(salonId, category.id).getOrNull() ?: continue
            services.firstOrNull { it.id == serviceId }?.let { return Result.success(it) }
        }
        return Result.success(null)
    }

    fun confirmBooking(
        salonId: String?,
        serviceId: String?,
        specialistId: String?,
        dateKey: String?,
        time: String?,
        onSuccess: (backendBookingId: String) -> Unit,
    ) {
        if (isSubmitting) return
        if (salonId == null || serviceId == null || specialistId == null || dateKey == null || time == null) {
            submitError = "اطلاعات نوبت کامل نیست. لطفاً دوباره تلاش کنید."
            return
        }
        isSubmitting = true
        submitError = null
        viewModelScope.launch {
            bookingRepository.createBooking(
                salonId = salonId,
                serviceId = serviceId,
                specialistId = specialistId,
                startTime = "${dateKey}T$time:00",
                notes = null,
                idempotencyKey = UUID.randomUUID().toString(),
            ).onSuccess { booking ->
                isSubmitting = false
                onSuccess(booking.id)
            }.onFailure { error ->
                isSubmitting = false
                submitError = userMessageFor(error)
            }
        }
    }
}
