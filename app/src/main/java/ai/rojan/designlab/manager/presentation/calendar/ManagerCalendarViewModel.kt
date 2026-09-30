package ai.rojan.designlab.manager.presentation.calendar

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.domain.repository.ActiveSalonContextRepository
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.BookingStatus
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Display-ready shape of one real salon booking. [customerLabel] is an
 * honest "—" placeholder, not a resolved name: the backend has no
 * endpoint that lets a salon owner resolve a booking's `customerId` to a
 * profile (confirmed absent — see `TEAM2_RESULT_MANAGER_DATA_PERSISTENCE.md`),
 * so there is nothing real to show here yet. This is the same
 * honest-unknown convention `AppointmentsViewModel`/`BookingConfirmationScreen`
 * already use for a failed lookup — never a fabricated customer name.
 */
data class ManagerCalendarAppointment(
    val id: String,
    val dateKey: String,
    val time: String,
    val specialistId: String,
    val specialistName: String,
    val serviceName: String,
    val customerLabel: String,
    val status: BookingStatus,
)

data class ManagerCalendarData(
    val salonId: String,
    val specialists: List<Specialist>,
    val appointments: List<ManagerCalendarAppointment>,
)

/**
 * TEAM2-002 (Manager Data Persistence). Backs
 * [ai.rojan.designlab.manager.screens.calendar.ManagerCalendarScreen] with
 * the salon's real bookings (`GET /api/v1/salons/{salonId}/bookings`, via
 * [BookingRepository.salonBookings]) — replacing the previous direct reads
 * of `manager.data.ManagerRepositories.appointments`/`.specialists`.
 *
 * Also owns the two booking-lifecycle actions the backend already
 * supports and TEAM2-003 already gave this app a real client for:
 * [confirmAppointment] (`PENDING` -> `CONFIRMED`) and [completeAppointment]
 * (`CONFIRMED` -> `COMPLETED`) — "Update booking status" from this task's
 * own priority list. Both reload from the backend afterward regardless of
 * outcome, same "the refreshed list is always the source of truth"
 * pattern `AppointmentsViewModel.cancelBooking` already established.
 *
 * Master Integration Repair, Pass 3 (Staff/Receptionist access): [load] previously resolved the
 * salon via the owner-only `salonRepository.myOwnedSalons()` (`GET /api/v1/salons/mine`), the same
 * endpoint [ai.rojan.designlab.manager.data.ManagerRepositories]'s own doc comment already documents
 * as returning empty for a genuine `SalonMembership`-based Manager/Receptionist - a non-owner staff
 * account saw a silent, unexplained empty Calendar despite `Permission.MANAGE_BOOKINGS` already
 * granting them real backend access. Now resolves the same way
 * [ai.rojan.designlab.manager.data.ManagerRepositories.loadFromBackend] does: [activeSalonContextRepository]'s
 * already-staff-inclusive active salon id (populated at login by
 * `ManagerAuthViewModel.resolveActiveSalon` from `availableSalons()` = owned + membership +
 * specialist link), then [SalonRepository.getSalon] - existence-only, not an ownership check.
 */
class ManagerCalendarViewModel(
    private val salonRepository: SalonRepository,
    private val bookingRepository: BookingRepository,
    private val specialistRepository: SpecialistRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
    private val activeSalonContextRepository: ActiveSalonContextRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<ManagerCalendarData>>(UiState.Loading)
        private set

    /** Non-null while a confirm/complete call for that booking id is in flight, so its row can disable itself against a double-tap. */
    var updatingBookingId by mutableStateOf<String?>(null)
        private set

    /** See [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel.requiresReauth]'s doc comment — same real-401 signal, same reason. */
    var requiresReauth by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load() {
        state = UiState.Loading
        requiresReauth = false
        viewModelScope.launch {
            val activeSalonId = activeSalonContextRepository.observeActiveSalonId().first()
            if (activeSalonId == null) {
                state = UiState.Empty
                return@launch
            }
            salonRepository.getSalon(activeSalonId)
                .onSuccess { salon -> loadForSalon(salon.id) }
                .onFailure { error -> handleFailure(error) }
        }
    }

    private fun handleFailure(error: Throwable) {
        if (error is BackendApiException && error.statusCode == 401) {
            requiresReauth = true
        } else {
            state = UiState.Error(userMessageFor(error))
        }
    }

    fun retry() = load()

    fun confirmAppointment(bookingId: String) = updateStatus(bookingId) { bookingRepository.confirmBooking(bookingId) }

    fun completeAppointment(bookingId: String) = updateStatus(bookingId) { bookingRepository.completeBooking(bookingId) }

    private fun updateStatus(bookingId: String, call: suspend () -> Result<*>) {
        if (updatingBookingId != null) return
        updatingBookingId = bookingId
        viewModelScope.launch {
            call()
            updatingBookingId = null
            load()
        }
    }

    private suspend fun loadForSalon(salonId: String) {
        val bookingsResult = bookingRepository.salonBookings(salonId, page = 0, size = CALENDAR_WINDOW_SIZE)
        bookingsResult
            .onSuccess { paged ->
                val specialists = specialistRepository.getSpecialists(salonId).getOrNull().orEmpty()
                state = if (paged.content.isEmpty()) {
                    UiState.Empty
                } else {
                    val specialistNames = specialists.associateBy { it.id }
                    val services = mutableMapOf<String, Service?>()
                    val appointments = paged.content.map { booking ->
                        val service = services.getOrPut(booking.serviceId) {
                            resolveService(salonId, booking.serviceId)
                        }
                        ManagerCalendarAppointment(
                            id = booking.id,
                            dateKey = booking.startTime.substringBefore('T'),
                            time = booking.startTime.substringAfter('T').take(5),
                            specialistId = booking.specialistId,
                            specialistName = specialistNames[booking.specialistId]?.displayName ?: "—",
                            serviceName = service?.name ?: "—",
                            customerLabel = "—",
                            status = booking.status,
                        )
                    }
                    UiState.Success(ManagerCalendarData(salonId = salonId, specialists = specialists, appointments = appointments))
                }
            }
            .onFailure { error -> handleFailure(error) }
    }

    private suspend fun resolveService(salonId: String, serviceId: String): Service? {
        val categories = serviceCategoryRepository.getCategories(salonId).getOrNull() ?: return null
        for (category in categories) {
            val services = serviceRepository.getServices(salonId, category.id).getOrNull() ?: continue
            services.firstOrNull { it.id == serviceId }?.let { return it }
        }
        return null
    }

    private companion object {
        /** No "load more" on this screen — same disclosed, bounded-page limitation as `AppointmentsViewModel`'s. */
        const val CALENDAR_WINDOW_SIZE = 200
    }
}
