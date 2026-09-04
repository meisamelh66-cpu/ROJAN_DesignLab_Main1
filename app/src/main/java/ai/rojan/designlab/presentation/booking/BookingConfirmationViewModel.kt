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
 * **Booking Transaction Integrity (TEAM2-001):** [onResult] in
 * [confirmBooking] is invoked if and only if the backend call both
 * succeeded and returned a booking with a non-blank persisted id — that id
 * is what [ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel.bookAppointment]
 * records and what a later real cancel needs. Any other outcome (network
 * failure, a non-2xx response, a malformed/incomplete response body, or a
 * blank id) is surfaced via [submitError] instead, and [onResult] is never
 * called — the caller (`BookingConfirmationScreen`/`RojanNavGraph`) must
 * not treat the booking as confirmed, must not navigate to the success
 * screen, and must not award any loyalty/wallet reward unless [onResult]
 * actually fires. This replaces an earlier "best-effort, not a gate"
 * design from when auth was frozen and this call was expected to 401
 * unconditionally; auth is wired for real now
 * (`di/BackendApiContainer.kt`), so a failure here is a real failure, not
 * an expected one, and can no longer be silently absorbed into the local
 * demo success flow.
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

    /**
     * Non-null exactly when the most recent [confirmBooking] attempt did
     * not produce a confirmed backend booking — a user-facing message,
     * ready to show via [ai.rojan.designlab.ui.components.state.RojanErrorState].
     * Cleared at the start of every new [confirmBooking] attempt (including
     * a retry of the same tap), so it never lingers past a subsequent
     * success.
     */
    var submitError by mutableStateOf<String?>(null)
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

    /**
     * [onResult] fires with the real backend `Booking.id` only when the
     * booking is genuinely confirmed and persisted. Every other outcome —
     * missing required selection state, a failed API call, or a response
     * that decoded but carries a blank id — sets [submitError] and returns
     * without calling [onResult]. Safe to call again after a failure: it
     * re-enters the same flow (a fresh attempt, per
     * [BookingRepository.createBooking]'s own idempotency-key contract)
     * and clears the previous [submitError] first.
     */
    fun confirmBooking(
        salonId: String?,
        serviceId: String?,
        specialistId: String?,
        dateKey: String?,
        time: String?,
        onResult: (backendBookingId: String) -> Unit,
    ) {
        if (isSubmitting) return
        submitError = null

        if (salonId == null || serviceId == null || specialistId == null || dateKey == null || time == null) {
            submitError = "اطلاعات لازم برای تکمیل رزرو در دسترس نیست. لطفاً دوباره تلاش کنید."
            return
        }

        isSubmitting = true
        viewModelScope.launch {
            bookingRepository.createBooking(
                salonId = salonId,
                serviceId = serviceId,
                specialistId = specialistId,
                startTime = "${dateKey}T$time:00",
                notes = null,
                idempotencyKey = UUID.randomUUID().toString(),
            )
                .onSuccess { booking ->
                    isSubmitting = false
                    if (booking.id.isBlank()) {
                        // The call returned 2xx and decoded, but without a
                        // usable booking id — treat exactly like a failure:
                        // there is nothing a caller could record or later
                        // cancel.
                        submitError = "پاسخ سرور نامعتبر بود. لطفاً دوباره تلاش کنید."
                    } else {
                        onResult(booking.id)
                    }
                }
                .onFailure { error ->
                    isSubmitting = false
                    submitError = userMessageFor(error)
                }
        }
    }
}
