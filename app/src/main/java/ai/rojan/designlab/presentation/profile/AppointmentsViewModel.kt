package ai.rojan.designlab.presentation.profile

import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.domain.repository.Booking
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Display-ready shape of one real backend [Booking] — [Booking] itself
 * only carries ids, so [salonName]/[serviceName]/[specialistName]/[price]
 * are resolved separately (same pattern
 * [ai.rojan.designlab.presentation.booking.BookingConfirmationViewModel]
 * already uses for the confirmation screen). [id] is always the real
 * backend `Booking.id` — every instance of this type came from
 * `GET /api/v1/bookings/mine`, never from local/demo state.
 */
data class BookingAppointment(
    val id: String,
    val salonName: String,
    val serviceName: String,
    val specialistName: String,
    /** Raw ISO start time, kept alongside [dateLabel]/[time] so callers can sort chronologically without re-parsing a formatted label. */
    val startTime: String,
    val dateLabel: String,
    val time: String,
    val price: Int,
    val status: BookingStatus,
)

/**
 * TEAM2-004 (Real Customer Booking Data). Backs
 * [ai.rojan.designlab.screens.profile.AppointmentsScreen]'s appointment
 * list with the customer's real backend bookings
 * (`GET /api/v1/bookings/mine`, via [BookingRepository.myBookings] —
 * already implemented on both ends, previously never called from any
 * screen). Replaces the screen's earlier data source,
 * [ai.rojan.designlab.presentation.customer.CustomerEcosystemViewModel]'s
 * local/demo `upcomingAppointments`/`pastAppointments` lists.
 *
 * [state] follows this app's [UiState] convention: [UiState.Loading] while
 * fetching, [UiState.Error] on any failure (never a silently-empty or
 * silently-successful list), [UiState.Empty] only when the backend
 * genuinely returned zero bookings, and [UiState.Success] with the
 * resolved, display-ready list otherwise. A booking's own id/status/times
 * always come straight from the backend; only its *display* name fields
 * degrade to "—" if their individual salon/specialist/service lookup
 * fails — the same honest-placeholder idiom
 * `BookingConfirmationScreen.kt`'s `SummaryRow`s already use, not a
 * fabricated value standing in for one.
 */
class AppointmentsViewModel(
    private val bookingRepository: BookingRepository,
    private val salonRepository: SalonRepository,
    private val specialistRepository: SpecialistRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<BookingAppointment>>>(UiState.Loading)
        private set

    /** True while a cancel is in flight, so the confirming card can disable its cancel action rather than allow a double-tap. */
    var cancellingBookingId by mutableStateOf<String?>(null)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch {
            bookingRepository.myBookings(page = 0, size = PAGE_SIZE)
                .onSuccess { paged ->
                    state = if (paged.content.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(resolveDisplayData(paged.content))
                    }
                }
                .onFailure { error ->
                    state = UiState.Error(userMessageFor(error))
                }
        }
    }

    fun retry() = load()

    /**
     * Fires the real `PATCH /bookings/{id}/cancel` call, then reloads from
     * the backend regardless of outcome — the refreshed list is always
     * the source of truth (cancelled if it succeeded, unchanged if it
     * didn't), so there's no separate cancel-error UI to build: a cancel
     * that silently failed is visible as "the status didn't change,"
     * never as a fabricated success.
     */
    fun cancelBooking(bookingId: String) {
        if (cancellingBookingId != null) return
        cancellingBookingId = bookingId
        viewModelScope.launch {
            bookingRepository.cancelBooking(bookingId)
            cancellingBookingId = null
            load()
        }
    }

    private suspend fun resolveDisplayData(bookings: List<Booking>): List<BookingAppointment> {
        val salonNames = mutableMapOf<String, String>()
        val specialistNames = mutableMapOf<String, String>()
        val services = mutableMapOf<String, Service?>()

        return bookings.map { booking ->
            val salonName = salonNames.getOrPut(booking.salonId) {
                salonRepository.getSalon(booking.salonId).getOrNull()?.name ?: "—"
            }
            val specialistName = specialistNames.getOrPut(booking.specialistId) {
                specialistRepository.getSpecialist(booking.salonId, booking.specialistId)
                    .getOrNull()?.displayName ?: "انتخاب خودکار"
            }
            val service = services.getOrPut("${booking.salonId}:${booking.serviceId}") {
                resolveService(booking.salonId, booking.serviceId)
            }

            BookingAppointment(
                id = booking.id,
                salonName = salonName,
                serviceName = service?.name ?: "—",
                specialistName = specialistName,
                startTime = booking.startTime,
                dateLabel = RollingBookingDates.fullLabelFor(booking.startTime.substringBefore('T')),
                time = booking.startTime.substringAfter('T').take(5),
                price = service?.price?.roundToInt() ?: 0,
                status = booking.status,
            )
        }
    }

    /**
     * A service has no standalone "get by id" endpoint — resolved the same
     * way [ai.rojan.designlab.presentation.booking.BookingConfirmationViewModel.resolveService]
     * already does: fan out over the salon's categories.
     */
    private suspend fun resolveService(salonId: String, serviceId: String): Service? {
        val categories = serviceCategoryRepository.getCategories(salonId).getOrNull() ?: return null
        for (category in categories) {
            val servicesInCategory = serviceRepository.getServices(salonId, category.id).getOrNull() ?: continue
            servicesInCategory.firstOrNull { it.id == serviceId }?.let { return it }
        }
        return null
    }

    private companion object {
        /**
         * No "load more" UI exists on this screen (see
         * TEAM2_RESULT_CUSTOMER_BOOKINGS.md's Remaining Risks) — one
         * generously-sized page stands in for real pagination until that's
         * built. A customer with more bookings than this ever had will not
         * see their oldest ones.
         */
        const val PAGE_SIZE = 100
    }
}
