package ai.rojan.designlab.manager.presentation.booking

import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.BookingRepository
import ai.rojan.designlab.domain.repository.SalonCustomer
import ai.rojan.designlab.domain.repository.SalonCustomerRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.manager.domain.booking.ManagerBookingState
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** The manager's own salon's real bookable catalog — everything [ManagerBookingViewModel]'s service/specialist steps need, loaded once per wizard session. */
data class ManagerBookingCatalog(
    val salonId: String,
    val services: List<Service>,
    val specialists: List<Specialist>,
)

/**
 * Manager Booking Journey — owns the in-progress [ManagerBookingState]
 * for the whole 7-screen wizard.
 *
 * Scoped to the wizard's own back-stack lifetime (shared across all 7
 * screens via `navController.getBackStackEntry(ManagerDestinations.CREATE_APPOINTMENT)`,
 * see [ai.rojan.designlab.manager.navigation.ManagerNavGraph]) — not an
 * app-lifetime singleton, so it's naturally cleared when the wizard
 * completes or is abandoned.
 *
 * **Manager Booking Creation Integrity follow-up:** every selection step
 * sources real backend data for the manager's own salon (`GET
 * /salons/mine` to resolve it, then the same
 * [ServiceRepository]/[SpecialistRepository]/[AvailabilityRepository] the
 * Customer booking flow already uses) instead of
 * `manager.data.ManagerRepositories`' in-memory catalog, and [confirm]
 * fires the real `POST /api/v1/bookings` with a real `customerId` —
 * resolved via the salon-scoped [SalonCustomerRepository] search, never a
 * fabricated identity. [confirm]'s `onSuccess` callback fires if and only
 * if the backend genuinely returns a persisted booking id — the exact "no
 * fake success" contract TEAM2-001 established for the Customer flow.
 *
 * Real backend [Specialist]/[SalonCustomer] carry no "skills"/"phone tag"
 * concept the old in-memory `manager.domain.specialist.Specialist`/
 * `manager.domain.customer.ManagerCustomer` models had — the specialist
 * step no longer filters by service (there is nothing real to filter on)
 * and the customer step shows a real name/email, not a name/phone/tag.
 * Disclosed simplifications, not silently dropped features: see
 * `TEAM2_RESULT_MANAGER_BOOKING_CREATION_V2.md`.
 *
 * **Process-death fix (5B6-1, preserved from Handoff):** the wizard's five
 * selection fields previously lived only in [MutableStateFlow], so a
 * manager who backgrounded the app mid-wizard (an ordinary interruption on
 * a shared salon device) returned — via the nested graph's restored
 * back-stack entry — into an empty wizard. [savedStateHandle] persists
 * `customerId`/`serviceId`/`specialistId`/`dateKey`/`time` on every
 * mutation via [setState] and [restoreState] rebuilds them on
 * construction; every other field (`isSubmitting`/`createdAppointmentId`/
 * `submitError`) is deliberately transient and not persisted. The
 * [SavedStateHandle] is sourced from the nested graph's
 * `NavBackStackEntry` extras at the `managerBookingViewModelFor` call
 * site — the API Navigation-Compose is built to restore through.
 */
class ManagerBookingViewModel(
    private val salonRepository: SalonRepository,
    private val salonCustomerRepository: SalonCustomerRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val serviceRepository: ServiceRepository,
    private val specialistRepository: SpecialistRepository,
    private val availabilityRepository: AvailabilityRepository,
    private val bookingRepository: BookingRepository,
    /** Defaults to a fresh, unattached handle so every existing positional/named call site (including this ViewModel's own non-SavedState-focused unit tests) keeps compiling unchanged; the real navigation call site always supplies the graph-restored one — see this class's own doc comment. */
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoreState())
    val uiState: StateFlow<ManagerBookingState> = _uiState.asStateFlow()

    var catalogState by mutableStateOf<UiState<ManagerBookingCatalog>>(UiState.Loading)
        private set

    var customerSearchState by mutableStateOf<UiState<List<SalonCustomer>>>(UiState.Empty)
        private set

    var slotsState by mutableStateOf<UiState<List<TimeSlot>>>(UiState.Empty)
        private set

    init {
        loadCatalog()
    }

    private fun restoreState(): ManagerBookingState = ManagerBookingState(
        customerId = savedStateHandle[KEY_CUSTOMER_ID],
        serviceId = savedStateHandle[KEY_SERVICE_ID],
        specialistId = savedStateHandle[KEY_SPECIALIST_ID],
        dateKey = savedStateHandle[KEY_DATE_KEY],
        time = savedStateHandle[KEY_TIME],
    )

    /** Single write path — updates the flow and persists the five real selection fields (not the transient submit-status ones). */
    private fun setState(newState: ManagerBookingState) {
        _uiState.value = newState
        savedStateHandle[KEY_CUSTOMER_ID] = newState.customerId
        savedStateHandle[KEY_SERVICE_ID] = newState.serviceId
        savedStateHandle[KEY_SPECIALIST_ID] = newState.specialistId
        savedStateHandle[KEY_DATE_KEY] = newState.dateKey
        savedStateHandle[KEY_TIME] = newState.time
    }

    /** Fresh state for a new booking session (also re-fetches the catalog, in case it failed or is stale from a previous attempt). */
    fun reset() {
        setState(ManagerBookingState())
        customerSearchState = UiState.Empty
        slotsState = UiState.Empty
        loadCatalog()
    }

    fun retryLoadCatalog() = loadCatalog()

    private fun loadCatalog() {
        catalogState = UiState.Loading
        viewModelScope.launch {
            salonRepository.myOwnedSalons()
                .onSuccess { salons ->
                    val salon = salons.firstOrNull()
                    if (salon == null) {
                        catalogState = UiState.Empty
                        return@launch
                    }
                    val specialists = specialistRepository.getSpecialists(salon.id).getOrElse {
                        catalogState = UiState.Error(userMessageFor(it))
                        return@launch
                    }
                    val services = allServices(salon.id)
                    catalogState = UiState.Success(ManagerBookingCatalog(salon.id, services, specialists))
                }
                .onFailure { catalogState = UiState.Error(userMessageFor(it)) }
        }
    }

    private suspend fun allServices(salonId: String): List<Service> {
        val categories = serviceCategoryRepository.getCategories(salonId).getOrNull().orEmpty()
        return categories.flatMap { category -> serviceRepository.getServices(salonId, category.id).getOrNull().orEmpty() }
    }

    /** [query] blank/empty is a valid search — the salon's whole customer roster. */
    fun searchCustomers(query: String) {
        val salonId = (catalogState as? UiState.Success)?.data?.salonId ?: return
        customerSearchState = UiState.Loading
        viewModelScope.launch {
            salonCustomerRepository.searchCustomers(salonId, query)
                .onSuccess { customers ->
                    customerSearchState = if (customers.isEmpty()) UiState.Empty else UiState.Success(customers)
                }
                .onFailure { customerSearchState = UiState.Error(userMessageFor(it)) }
        }
    }

    fun selectCustomer(customerId: String) {
        setState(_uiState.value.copy(customerId = customerId))
    }

    fun selectService(serviceId: String) {
        setState(_uiState.value.copy(serviceId = serviceId))
    }

    fun selectSpecialist(specialistId: String) {
        setState(_uiState.value.copy(specialistId = specialistId))
    }

    /** Changing the date invalidates a previously chosen time and re-fetches this specialist's real availability for the new date. */
    fun selectDate(dateKey: String) {
        setState(_uiState.value.copy(dateKey = dateKey, time = null))
        loadSlots()
    }

    /** [time] must be a raw ISO-8601 `start` value from a real [availableTimes]-equivalent result — see [ManagerBookingState.time]'s doc comment for why. */
    fun selectTime(time: String) {
        setState(_uiState.value.copy(time = time))
    }

    fun retryLoadSlots() = loadSlots()

    private fun loadSlots() {
        val salonId = (catalogState as? UiState.Success)?.data?.salonId
        val state = _uiState.value
        val specialistId = state.specialistId
        val serviceId = state.serviceId
        val dateKey = state.dateKey
        if (salonId == null || specialistId == null || serviceId == null || dateKey == null) return

        slotsState = UiState.Loading
        viewModelScope.launch {
            availabilityRepository.getAvailableSlots(salonId, specialistId, serviceId, dateKey)
                .onSuccess { slots -> slotsState = if (slots.isEmpty()) UiState.Empty else UiState.Success(slots) }
                .onFailure { slotsState = UiState.Error(userMessageFor(it)) }
        }
    }

    fun customerById(id: String?): SalonCustomer? = id?.let { target ->
        (customerSearchState as? UiState.Success)?.data?.find { it.id == target }
    }

    fun serviceById(id: String?): Service? = id?.let { target ->
        (catalogState as? UiState.Success)?.data?.services?.find { it.id == target }
    }

    fun specialistById(id: String?): Specialist? = id?.let { target ->
        (catalogState as? UiState.Success)?.data?.specialists?.find { it.id == target }
    }

    /**
     * Real `POST /api/v1/bookings` with the selected real `customerId` —
     * the backend attributes the booking to that customer, not to the
     * manager (owner-only, enforced server-side; see
     * `SalonCustomerController`/`BookingController.resolveBookingCustomerId`
     * in `ROJAN_Backend`). [onSuccess] fires if and only if this call
     * genuinely succeeds and returns a persisted booking id — never
     * unconditionally. Any failure (network, validation, a slot taken in
     * the meantime, an inactive/deleted service or specialist) sets
     * [ManagerBookingState.submitError] instead and [onSuccess] is never
     * called — no local-only fallback of any kind.
     */
    fun confirm(onSuccess: () -> Unit) {
        val salonId = (catalogState as? UiState.Success)?.data?.salonId
        val state = _uiState.value
        if (state.isSubmitting) return

        val customerId = state.customerId
        val serviceId = state.serviceId
        val specialistId = state.specialistId
        val dateKey = state.dateKey
        val time = state.time
        if (salonId == null || customerId == null || serviceId == null || specialistId == null || dateKey == null || time == null) {
            setState(state.copy(submitError = "اطلاعات نوبت کامل نیست. لطفاً همه موارد را انتخاب کنید."))
            return
        }

        setState(state.copy(isSubmitting = true, submitError = null))
        viewModelScope.launch {
            bookingRepository.createBooking(
                salonId = salonId,
                serviceId = serviceId,
                specialistId = specialistId,
                startTime = "${dateKey}T$time:00",
                notes = null,
                idempotencyKey = UUID.randomUUID().toString(),
                customerId = customerId,
            )
                .onSuccess { booking ->
                    setState(_uiState.value.copy(isSubmitting = false, createdAppointmentId = booking.id))
                    onSuccess()
                }
                .onFailure { error ->
                    setState(_uiState.value.copy(isSubmitting = false, submitError = userMessageFor(error)))
                }
        }
    }

    private companion object {
        const val KEY_CUSTOMER_ID = "manager_booking_customer_id"
        const val KEY_SERVICE_ID = "manager_booking_service_id"
        const val KEY_SPECIALIST_ID = "manager_booking_specialist_id"
        const val KEY_DATE_KEY = "manager_booking_date_key"
        const val KEY_TIME = "manager_booking_time"
    }
}
