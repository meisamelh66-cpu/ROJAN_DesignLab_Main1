package ai.rojan.designlab.manager.presentation.dashboard

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.domain.booking.RollingBookingDates
import ai.rojan.designlab.domain.repository.ActiveSalonContextRepository
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.SalonOnboardingStatus
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.data.UpcomingSlot
import ai.rojan.designlab.manager.domain.booking.managerBookingTimeSlots
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
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
    // Salon Activation Awareness (Sprint 1 / P0 Master Integration Repair,
    // PASS 1): real backend `onboardingStatus`
    val onboardingStatus: SalonOnboardingStatus?,
    val todaysUpcomingSlots: List<UpcomingSlot> = emptyList(),
)

/**
 * Backs [ai.rojan.designlab.manager.screens.dashboard.ManagerDashboardScreen]'s
 * salon identity + "today's overview" sections with the active salon's
 * real data and bookings. Targets the salon currently selected in
 * [ActiveSalonContextRepository] rather than assuming the first owned salon.
 */
class ManagerDashboardViewModel(
    private val activeSalonContextRepository: ActiveSalonContextRepository,
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

    private var currentSalonId: String? = null

    init {
        viewModelScope.launch {
            activeSalonContextRepository.observeActiveSalonId()
                .collectLatest { salonId ->
                    currentSalonId = salonId
                    if (salonId.isNullOrBlank()) {
                        state = UiState.Empty
                    } else {
                        state = UiState.Loading
                        requiresReauth = false
                        loadSalon(salonId)
                    }
                }
        }
    }

    fun load() {
        val salonId = currentSalonId
        if (salonId.isNullOrBlank()) {
            viewModelScope.launch {
                val id = activeSalonContextRepository.observeActiveSalonId().firstOrNull()
                currentSalonId = id
                if (id.isNullOrBlank()) {
                    state = UiState.Empty
                } else {
                    state = UiState.Loading
                    requiresReauth = false
                    loadSalon(id)
                }
            }
        } else {
            state = UiState.Loading
            requiresReauth = false
            viewModelScope.launch {
                loadSalon(salonId)
            }
        }
    }

    fun retry() = load()

    private suspend fun loadSalon(salonId: String) {
        salonRepository.getSalon(salonId)
            .onSuccess { salon ->
                val (stats, slots) = loadStatsAndSlots(salon.id)
                state = UiState.Success(
                    ManagerDashboardData(
                        salonId = salon.id,
                        salonName = salon.name,
                        salonDescription = salon.description,
                        isActive = salon.active,
                        stats = stats,
                        onboardingStatus = salon.onboardingStatus,
                        todaysUpcomingSlots = slots,
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

    private suspend fun loadStatsAndSlots(salonId: String): Pair<ManagerDashboardStats, List<UpcomingSlot>> {
        val bookings = bookingRepository.salonBookings(salonId, page = 0, size = STATS_WINDOW_SIZE)
            .getOrNull()?.content.orEmpty()
        val specialists = specialistRepository.getSpecialists(salonId).getOrNull().orEmpty()
        val services = allServices(salonId).associateBy { it.id }

        val todayIso = RollingBookingDates.next7Days().first().first
        val todays = bookings.filter { it.startTime.substringBefore('T') == todayIso }
        val todaysActive = todays.filter { it.status != BookingStatus.CANCELLED }

        val revenue = todaysActive.sumOf { services[it.serviceId]?.price ?: 0.0 }
        val totalCapacity = (specialists.size * managerBookingTimeSlots.size).coerceAtLeast(1)
        val occupancy = ((todaysActive.size * 100) / totalCapacity).coerceIn(0, 100)

        val stats = ManagerDashboardStats(
            todaysAppointmentCount = todays.size,
            todaysRevenueLabel = formatRevenueLabel(revenue),
            occupancyPercent = occupancy,
        )

        val slots = todaysActive
            .sortedBy { it.startTime }
            .map { booking ->
                UpcomingSlot(
                    time = booking.startTime.substringAfter('T').take(5),
                    clientName = "—",
                    service = services[booking.serviceId]?.name ?: "—",
                )
            }

        return Pair(stats, slots)
    }

    private suspend fun allServices(salonId: String): List<Service> {
        val categories = serviceCategoryRepository.getCategories(salonId).getOrNull().orEmpty()
        return categories.flatMap { category -> serviceRepository.getServices(salonId, category.id).getOrNull().orEmpty() }
    }

    private fun formatRevenueLabel(revenue: Double): String =
        if (revenue >= 1_000_000) "%.1f".format(revenue / 1_000_000.0) + "م" else "${(revenue / 1000).toInt()}ت"

    private companion object {
        /** No "load more" on this screen — one bounded page stands in for real pagination, same disclosed limitation as `AppointmentsViewModel`'s. Bounded by the backend's `PageRequest.MAX_SIZE` (100) — a larger size is rejected with 400 INVALID_ARGUMENT. */
        const val STATS_WINDOW_SIZE = 100
    }
}
