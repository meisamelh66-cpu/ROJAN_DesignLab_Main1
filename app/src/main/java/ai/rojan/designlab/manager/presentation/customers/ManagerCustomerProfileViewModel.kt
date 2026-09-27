package ai.rojan.designlab.manager.presentation.customers

import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.domain.repository.availableSalons
import ai.rojan.designlab.manager.domain.customer.CustomerNote
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * One real customer profile: identity plus the two per-customer detail lists
 * [CustomerRepository.loadDetail] populates. `history`/`notes` are already fully resolved,
 * real, and pre-formatted by [ai.rojan.designlab.manager.data.BackendCustomerRepository] (service/
 * specialist names and price already cross-referenced, notes already newest-first) - this data class
 * adds nothing, it only names the bundle [state] carries.
 */
data class ManagerCustomerProfileData(
    val customer: ManagerCustomer,
    val history: List<CustomerServiceHistoryEntry>,
    val notes: List<CustomerNote>,
)

/**
 * Manager Customer Profile.
 *
 * **Phase 3A retarget (Customer Architecture Decision Audit):** previously depended on
 * [ai.rojan.designlab.domain.repository.SalonCustomerRepository] and called `getCustomer`/
 * `getCustomerBookings`/`getCustomerNotes` — methods that do not exist on that interface's real,
 * current shape (it only declares `searchCustomers`, for the Booking wizard's customer picker).
 * That made this file fail to compile against the actual repository. Retargeted onto
 * [CustomerRepository]/[ManagerCustomer] — the exact stack the currently shipped
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomerProfileScreen] already uses via
 * [ai.rojan.designlab.manager.data.ManagerRepositories.customers] — using its real
 * [CustomerRepository.getById]/[CustomerRepository.getServiceHistory]/
 * [CustomerRepository.getNoteHistory]/[CustomerRepository.loadDetail] operations, which already
 * exist and already work. `serviceCategoryRepository`/`serviceRepository`/`specialistRepository`
 * and the old `ManagerCustomerHistoryEntry`/price-formatting helpers are gone — no longer needed,
 * since [CustomerRepository.getServiceHistory] already returns fully resolved, pre-formatted
 * entries; the old cross-referencing logic they existed for is redundant now, not lost.
 *
 * **CRITICAL P0 safety contract, preserved:** the commit that fixed
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomerProfileScreen] (identity
 * substitution / silent wrong-customer fallback) is a *screen-local* fix and is not modified or
 * replaced by this ViewModel — the screen is not wired to this ViewModel in this phase. This
 * ViewModel independently upholds the same contract for when it eventually is: [customerId] is the
 * only identity source passed to every repository call below; [load] never falls back to
 * `getAll().firstOrNull()` or any other customer; a [CustomerRepository.getById] miss (after a
 * successful [CustomerRepository.loadDetail]) is an explicit [UiState.Empty] "not found" — the same
 * convention this ViewModel family already used for a genuine backend 404 before this retarget —
 * and a real [CustomerRepository.loadDetail] failure is always [UiState.Error], never silently
 * reinterpreted as "not found."
 *
 * **Manager Salon Access Alignment (preserved):** salon *reachability* is still resolved through
 * [CurrentUserIdentityContextRepository]/[availableSalons] — the same ownership-OR-membership-OR-
 * specialist check [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel]
 * uses. [state] is [UiState.Empty] both when no salon is reachable at all and on a genuine
 * customer-not-found; [noAccessibleSalon] disambiguates which.
 */
class ManagerCustomerProfileViewModel(
    private val customerRepository: CustomerRepository,
    private val currentUserIdentityContextRepository: CurrentUserIdentityContextRepository,
    private val customerId: String,
) : ViewModel() {

    var state by mutableStateOf<UiState<ManagerCustomerProfileData>>(UiState.Loading)
        private set

    /** True only while [state] is [UiState.Empty] because no salon is reachable through this account's real access at all - never on a genuine customer-not-found. */
    var noAccessibleSalon by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        state = UiState.Loading
        noAccessibleSalon = false
        viewModelScope.launch {
            val context = currentUserIdentityContextRepository.getCurrentUserIdentityContext().getOrElse {
                state = UiState.Error(userMessageFor(it))
                return@launch
            }
            if (context.availableSalons().isEmpty()) {
                noAccessibleSalon = true
                state = UiState.Empty
                return@launch
            }

            customerRepository.loadDetail(customerId).onFailure { error ->
                state = UiState.Error(userMessageFor(error))
                return@launch
            }

            // customerId is the only identity source below - never a substitute customer.
            val customer = customerRepository.getById(customerId)
            if (customer == null) {
                state = UiState.Empty
                return@launch
            }

            state = UiState.Success(
                ManagerCustomerProfileData(
                    customer = customer,
                    history = customerRepository.getServiceHistory(customerId),
                    notes = customerRepository.getNoteHistory(customerId),
                ),
            )
        }
    }
}
