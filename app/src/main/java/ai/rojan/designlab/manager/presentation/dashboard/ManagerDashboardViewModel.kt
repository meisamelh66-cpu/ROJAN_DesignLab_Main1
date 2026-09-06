package ai.rojan.designlab.manager.presentation.dashboard

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.booking.managerBookingTimeSlots
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import ai.rojan.designlab.ui.money.toTomanLong
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/** Real per-salon dashboard numbers — replaces `manager.data.computeManagerDashboardStats()`'s in-memory computation with the same shape, computed from real backend bookings/specialists/services. */
data class ManagerDashboardStats(
    val todaysAppointmentCount: Int,
    val todaysRevenueLabel: String,
    val occupancyPercent: Int,
)

data class ManagerDashboardData(
    val salonId: String,
    val salonName: String,
    val salonDescription: String?,
    val isActive: Boolean,
    val stats: ManagerDashboardStats,
)

/**
 * TEAM2-002 (Manager Data Persistence). Backs
 * [ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen]'s
 * salon identity + "today's overview" sections with the authenticated
 * manager's real salon and real bookings — replacing
 * `SalonIdentityCard`'s hardcoded default params and
 * `TodayOverviewSection`'s `computeManagerDashboardStats()` (which read
 * `manager.data.ManagerRepositories`' in-memory singletons).
 *
 * A manager account can in principle own more than one salon
 * (`GET /api/v1/salons/mine` returns a list) — this MVP, like the rest of
 * the Manager app, assumes a single salon and uses the first one; a real
 * salon switcher is new UI this task doesn't add (disclosed simplification,
 * see `TEAM2_RESULT_MANAGER_DATA_PERSISTENCE.md`). Zero salons owned is a
 * real [UiState.Empty], not an error — a legitimate account state.
 *
 * There is no backend "revenue"/"occupancy" endpoint; both are computed
 * here the same way `computeManagerDashboardStats()` always did — a
 * genuine (if simplified) ratio/sum over real data, not a fabricated
 * number — just sourced from [BookingRepository.salonBookings] instead of
 * an in-memory list. "New customers today" is dropped: it required
 * `manager.data.ManagerRepositories.customers`, which has no backend
 * equivalent (no endpoint resolves a booking's customer to a profile —
 * see the Calendar-side of this same task's report for the same gap).
 */
class ManagerDashboardViewModel(
    private val salonRepository: SalonRepository,
    private val bookingRepository: BookingRepository,
    private val specialistRepository: SpecialistRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<ManagerDashboardData>>(UiState.Loading)
        private set

    /**
     * True exactly when the most recent load failed with a real 401 — the
     * refresh token itself is dead (`TokenAuthenticator` already tried and
     * failed to silently refresh before this ever surfaces), so a retry
     * button that re-fires the same request would just fail again. The
     * screen observes this and routes back to the login gate instead of
     * showing [state] as a generic, retriable [UiState.Error].
     */
    var requiresReauth by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        requiresReauth = false
        viewModelScope.launch {
            salonRepository.myOwnedSalons()
                .onSuccess { salons ->
                    val salon = salons.firstOrNull()
                    if (salon == null) {
                        state = UiState.Empty
                        return@launch
                    }
                    state = UiState.Success(
                        ManagerDashboardData(
                            salonId = salon.id,
                            salonName = salon.name,
                            salonDescription = salon.description,
                            isActive = salon.active,
                            stats = loadStats(salon.id),
                        ),
                    )
                }
                .onFailure { error ->
                    if (error is BackendApiException && error.statusCode == 401) {
                        requiresReauth = true
                    } else {
                        state = UiState.Error(userMessageFor(error))
                    }
                }
        }
    }

    fun retry() = load()

    private suspend fun loadStats(salonId: String): ManagerDashboardStats {
        val bookings = bookingRepository.salonBookings(salonId, page = 0, size = STATS_WINDOW_SIZE)
            .getOrNull()?.content.orEmpty()
        val specialists = specialistRepository.getSpecialists(salonId).getOrNull().orEmpty()
        val services = allServices(salonId).associateBy { it.id }

        val todayIso = RollingBookingDates.next7Days().first().first
        val todays = bookings.filter { it.startTime.substringBefore('T') == todayIso }
        val todaysActive = todays.filter { it.status != BookingStatus.CANCELLED }

        // FIX-005: accumulate revenue as whole Toman (Long), rounding each
        // service price once at the point it enters the sum, rather than
        // adding raw Doubles and letting binary-FP error compound across
        // the day's bookings. No backend "revenue" endpoint exists; this
        // is still the same client-side computation, just done safely.
        val revenue: Long = todaysActive.sumOf { (services[it.serviceId]?.price ?: 0.0).toTomanLong() }
        val totalCapacity = (specialists.size * managerBookingTimeSlots.size).coerceAtLeast(1)
        val occupancy = ((todaysActive.size * 100) / totalCapacity).coerceIn(0, 100)

        return ManagerDashboardStats(
            todaysAppointmentCount = todays.size,
            todaysRevenueLabel = formatRevenueLabel(revenue),
            occupancyPercent = occupancy,
        )
    }

    private suspend fun allServices(salonId: String): List<Service> {
        val categories = serviceCategoryRepository.getCategories(salonId).getOrNull().orEmpty()
        return categories.flatMap { category -> serviceRepository.getServices(salonId, category.id).getOrNull().orEmpty() }
    }

    private fun formatRevenueLabel(revenue: Long): String =
        if (revenue >= 1_000_000) "%.1f".format(revenue / 1_000_000.0) + "م" else "${(revenue / 1000).toInt()}ت"

    private companion object {
        /** No "load more" on this screen — one bounded page stands in for real pagination, same disclosed limitation as `AppointmentsViewModel`'s. */
        const val STATS_WINDOW_SIZE = 200
    }
}
