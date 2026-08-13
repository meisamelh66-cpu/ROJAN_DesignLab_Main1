package ai.rojan.designlab.reception.presentation.booking

import ai.rojan.designlab.data.remote.BackendApiException
import ai.rojan.designlab.domain.repository.AvailabilityRepository
import ai.rojan.designlab.domain.repository.Service
import ai.rojan.designlab.domain.repository.ServiceCategoryRepository
import ai.rojan.designlab.domain.repository.ServiceRepository
import ai.rojan.designlab.domain.repository.Specialist
import ai.rojan.designlab.domain.repository.SpecialistRepository
import ai.rojan.designlab.domain.repository.TimeSlot
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.reception.domain.booking.ReceptionBookingState
import ai.rojan.designlab.reception.domain.repository.ReceptionBookingRepository
import ai.rojan.designlab.reception.domain.repository.ReceptionCustomer
import ai.rojan.designlab.reception.domain.repository.ReceptionCustomerRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the in-progress [ReceptionBookingState] for the whole wizard and
 * every piece of business logic it needs — same role as
 * [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel],
 * deliberately simpler: every list (customers/services/specialists/times)
 * is a directly `suspend`-loaded [UiState], not a synchronous read from an
 * app-lifetime cache object. [salonId] is supplied at construction,
 * already resolved (see [ai.rojan.designlab.reception.data.ReceptionRepositories]'s
 * own doc comment for why this is safe here unlike `ManagerRepositories`).
 *
 * Every repository call here is real — `bookingRepository`/`customerRepository`
 * hit owner-only endpoints today and will legitimately fail with an
 * authorization error until `ROJAN_System1_Backend_Decision_v2.md` §4 item
 * 6 ships; `serviceRepository`/`specialistRepository`/`availabilityRepository`
 * are "any authenticated user" and work today. No mock, no fake data
 * anywhere in this class.
 */
class ReceptionBookingViewModel(
    private val salonId: String,
    private val bookingRepository: ReceptionBookingRepository,
    private val customerRepository: ReceptionCustomerRepository,
    private val serviceRepository: ServiceRepository,
    private val serviceCategoryRepository: ServiceCategoryRepository,
    private val specialistRepository: SpecialistRepository,
    private val availabilityRepository: AvailabilityRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceptionBookingState())
    val uiState: StateFlow<ReceptionBookingState> = _uiState.asStateFlow()

    private val _customers = MutableStateFlow<UiState<List<ReceptionCustomer>>>(UiState.Loading)
    val customers: StateFlow<UiState<List<ReceptionCustomer>>> = _customers.asStateFlow()

    private val _services = MutableStateFlow<UiState<List<Service>>>(UiState.Loading)
    val services: StateFlow<UiState<List<Service>>> = _services.asStateFlow()

    private val _specialists = MutableStateFlow<UiState<List<Specialist>>>(UiState.Loading)
    val specialists: StateFlow<UiState<List<Specialist>>> = _specialists.asStateFlow()

    private val _availableTimes = MutableStateFlow<UiState<List<TimeSlot>>>(UiState.Loading)
    val availableTimes: StateFlow<UiState<List<TimeSlot>>> = _availableTimes.asStateFlow()

    fun searchCustomers(query: String?) {
        _customers.value = UiState.Loading
        viewModelScope.launch {
            customerRepository.listCustomers(salonId, search = query)
                .onSuccess { result ->
                    _customers.value = if (result.content.isEmpty()) UiState.Empty else UiState.Success(result.content)
                }
                .onFailure { error -> _customers.value = UiState.Error(bookingErrorMessage(error)) }
        }
    }

    /**
     * Fetches every category's services and flattens them into one picker
     * list — the backend has no single "all services for a salon"
     * endpoint, only per-category.
     *
     * Per `ROJAN_Reception_Phase1_Review_Report_v1.md` §4 finding 1: a
     * category whose own `getServices` call fails must never be silently
     * dropped from the result while the categories that did succeed are
     * shown as a seemingly-complete [UiState.Success] — a receptionist
     * searching for a service in the failed category would otherwise see
     * no error and reasonably conclude it doesn't exist. Any failure,
     * partial or total, now surfaces as [UiState.Error] instead — never a
     * silently-incomplete success. [loadServices] is itself the retry
     * path (idempotent, safe to call again), surfaced via
     * [ai.rojan.designlab.reception.components.ReceptionUiStateList]'s
     * `onRetryClick`.
     */
    fun loadServices() {
        _services.value = UiState.Loading
        viewModelScope.launch {
            serviceCategoryRepository.getCategories(salonId)
                .fold(
                    onSuccess = { categories ->
                        val all = mutableListOf<Service>()
                        val failures = mutableListOf<Throwable>()
                        for (category in categories) {
                            serviceRepository.getServices(salonId, category.id)
                                .onSuccess { all += it }
                                .onFailure { failures += it }
                        }
                        _services.value = when {
                            failures.isNotEmpty() && all.isEmpty() -> UiState.Error(bookingErrorMessage(failures.first()))
                            failures.isNotEmpty() -> UiState.Error(
                                "دریافت برخی خدمات (${failures.size} از ${categories.size} دسته) با خطا مواجه شد. لطفاً دوباره تلاش کنید.",
                            )
                            all.isEmpty() -> UiState.Empty
                            else -> UiState.Success(all)
                        }
                    },
                    onFailure = { error -> _services.value = UiState.Error(bookingErrorMessage(error)) },
                )
        }
    }

    fun loadSpecialists() {
        _specialists.value = UiState.Loading
        viewModelScope.launch {
            specialistRepository.getSpecialists(salonId)
                .onSuccess { result ->
                    _specialists.value = if (result.isEmpty()) UiState.Empty else UiState.Success(result)
                }
                .onFailure { error -> _specialists.value = UiState.Error(bookingErrorMessage(error)) }
        }
    }

    fun loadAvailableTimes(dateIso: String) {
        val specialistId = _uiState.value.specialist?.id
        val serviceId = _uiState.value.service?.id
        if (specialistId == null || serviceId == null) {
            _availableTimes.value = UiState.Error("ابتدا خدمت و متخصص را انتخاب کنید")
            return
        }
        _uiState.value = _uiState.value.copy(dateIso = dateIso, time = null)
        _availableTimes.value = UiState.Loading
        viewModelScope.launch {
            availabilityRepository.getAvailableSlots(
                salonId = salonId,
                specialistId = specialistId,
                serviceId = serviceId,
                date = dateIso,
            ).onSuccess { slots ->
                _availableTimes.value = if (slots.isEmpty()) UiState.Empty else UiState.Success(slots)
            }.onFailure { error -> _availableTimes.value = UiState.Error(bookingErrorMessage(error)) }
        }
    }

    fun selectCustomer(customer: ReceptionCustomer) {
        _uiState.value = _uiState.value.copy(customer = customer)
    }

    fun selectService(service: Service) {
        _uiState.value = _uiState.value.copy(service = service)
    }

    fun selectSpecialist(specialist: Specialist) {
        _uiState.value = _uiState.value.copy(specialist = specialist)
    }

    /** [time] must be a raw ISO-8601 `start` value from a real [availableTimes] result — see [ReceptionBookingState.time]'s doc comment for why. */
    fun selectTime(time: String) {
        _uiState.value = _uiState.value.copy(time = time)
    }

    suspend fun confirm(): Result<String> {
        val state = _uiState.value
        val customerId = state.customer?.id ?: return Result.failure(IllegalStateException("مشتری انتخاب نشده است"))
        val serviceId = state.service?.id ?: return Result.failure(IllegalStateException("خدمت انتخاب نشده است"))
        val specialistId = state.specialist?.id ?: return Result.failure(IllegalStateException("متخصص انتخاب نشده است"))
        val startTime = state.time ?: return Result.failure(IllegalStateException("زمان انتخاب نشده است"))

        _uiState.value = state.copy(isSubmitting = true, confirmError = null)
        val result = bookingRepository.createBookingForCustomer(
            salonId = salonId,
            customerId = customerId,
            serviceId = serviceId,
            specialistId = specialistId,
            startTime = startTime,
            notes = null,
        )
        _uiState.value = _uiState.value.copy(
            isSubmitting = false,
            createdBookingId = result.getOrNull()?.id,
            confirmError = result.exceptionOrNull()?.let(::bookingErrorMessage),
        )
        return result.map { it.id }
    }
}

/**
 * Maps a real failure to Persian, user-facing copy — same distinguished-
 * error-code approach as
 * [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel]'s
 * own `confirmErrorMessage`, plus the authorization case Reception (not
 * yet an owner) is expected to actually hit today.
 */
private fun bookingErrorMessage(error: Throwable): String {
    val apiError = (error as? BackendApiException)?.apiError
    return when {
        (error as? BackendApiException)?.statusCode == 403 ->
            "دسترسی شما برای این عملیات هنوز فعال نشده است — این یک محدودیت شناخته‌شده است، نه خطای برنامه."
        apiError?.errorCode == "CUSTOMER_NOT_LINKED_TO_ACCOUNT" -> "این مشتری به حساب کاربری متصل نیست و امکان ثبت نوبت برای او وجود ندارد."
        apiError?.errorCode == "BOOKING_CONFLICT" -> "این بازه زمانی دیگر در دسترس نیست. لطفاً ساعت دیگری را انتخاب کنید."
        apiError?.errorCode in setOf("SPECIALIST_NOT_FOUND", "SERVICE_NOT_FOUND", "CUSTOMER_NOT_FOUND") -> "اطلاعات انتخاب‌شده دیگر معتبر نیست. لطفاً دوباره تلاش کنید."
        else -> apiError?.message ?: "درخواست با خطا مواجه شد. لطفاً دوباره تلاش کنید."
    }
}
