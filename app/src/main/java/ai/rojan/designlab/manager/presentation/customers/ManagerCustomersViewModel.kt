package ai.rojan.designlab.manager.presentation.customers

import ai.rojan.designlab.domain.repository.SalonCustomer
import ai.rojan.designlab.domain.repository.SalonCustomerRepository
import ai.rojan.designlab.domain.repository.SalonRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * FIX-004 (PARTIAL — Manager Customers list/search only).
 *
 * Backs [ai.rojan.designlab.manager.screens.customers.ManagerCustomersListScreen]
 * with the salon's real customer roster — `GET /api/v1/salons/{salonId}/customers`
 * via [SalonCustomerRepository.searchCustomers], the same real, salon-scoped
 * source [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel]
 * already uses for the booking wizard's customer step. Replaces the in-memory
 * [ai.rojan.designlab.manager.data.InMemoryCustomerRepository] sample roster
 * that screen read before.
 *
 * [state] follows this app's [UiState] convention: [UiState.Loading] while
 * fetching, [UiState.Error] on any failure (never a silently-empty or
 * silently-successful list), [UiState.Empty] when the salon genuinely has
 * no matching customers (or the account owns no salon), [UiState.Success]
 * with the real roster otherwise.
 *
 * **Out of FIX-004 scope, unchanged and still in-memory:** the customer
 * profile screen, service history, manager notes, loyalty score, visit
 * counts, phone number and customer tag. A real [SalonCustomer] carries
 * only id / email / fullName, and the backend exposes no endpoint for the
 * rest — see the FIX-004 STEP 1 investigation report.
 */
class ManagerCustomersViewModel(
    private val salonRepository: SalonRepository,
    private val salonCustomerRepository: SalonCustomerRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<SalonCustomer>>>(UiState.Loading)
        private set

    /** Resolved once from `GET /salons/mine`, then reused for every search. */
    private var cachedSalonId: String? = null

    init {
        search("")
    }

    /** [query] blank/empty is a valid search — the salon's whole roster. */
    fun search(query: String) {
        state = UiState.Loading
        viewModelScope.launch {
            val salonId = resolveSalonId() ?: return@launch
            salonCustomerRepository.searchCustomers(salonId, query)
                .onSuccess { customers ->
                    state = if (customers.isEmpty()) UiState.Empty else UiState.Success(customers)
                }
                .onFailure { state = UiState.Error(userMessageFor(it)) }
        }
    }

    /**
     * Resolves (and caches) the manager's own salon id — the same
     * `GET /salons/mine` → first owned salon resolution
     * [ai.rojan.designlab.manager.presentation.booking.ManagerBookingViewModel]
     * does. On a failure or a no-owned-salon result it sets [state]
     * (Error / Empty) and returns null so the caller stops.
     */
    private suspend fun resolveSalonId(): String? {
        cachedSalonId?.let { return it }
        return salonRepository.myOwnedSalons().fold(
            onSuccess = { salons ->
                val salon = salons.firstOrNull()
                if (salon == null) {
                    state = UiState.Empty
                    null
                } else {
                    salon.id.also { cachedSalonId = it }
                }
            },
            onFailure = {
                state = UiState.Error(userMessageFor(it))
                null
            },
        )
    }
}
