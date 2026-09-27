package ai.rojan.designlab.manager.presentation.customers

import ai.rojan.designlab.domain.phone.normalizeIranianPhoneNumber
import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.domain.repository.availableSalons
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import ai.rojan.designlab.presentation.common.UiState
import ai.rojan.designlab.presentation.common.userMessageFor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manager Customers List.
 *
 * **Phase 3A retarget (Customer Architecture Decision Audit):** previously depended on
 * [ai.rojan.designlab.domain.repository.SalonCustomerRepository]/[ai.rojan.designlab.domain.repository.SalonCustomer]
 * — the Booking wizard's thin customer-picker stack (`id`/`email`/`fullName` only; no phone, no
 * tag). That model can't express this screen's real behavior (phone display, phone search, tag
 * filtering), because the endpoint mapping it uses deliberately drops those fields for the picker's
 * narrower needs. Retargeted onto [CustomerRepository]/[ManagerCustomer] — the exact stack
 * [ai.rojan.designlab.manager.data.ManagerRepositories.customers] (the currently shipped screen)
 * already uses, so every field the real UI needs is actually present. The Booking wizard's own
 * `SalonCustomerRepository`/`SalonCustomer` stack is completely untouched by this change.
 *
 * [CustomerRepository]'s read methods take no `salonId` (the injected instance is already bound to
 * one salon) and are synchronous — a local, already-synced cache read, not a network call (see
 * [ai.rojan.designlab.manager.data.BackendCustomerRepository.search]) — unlike the old
 * `SalonCustomerRepository.searchCustomers`, which was `suspend` and salonId-parameterized.
 * [resolveSalonAccess] is kept purely for its [noAccessibleSalon] disambiguation: "does this account
 * have any real salon access at all" is still a distinct, real question from "did this search match
 * anything," independent of whether the actual customer read itself needs a salon id.
 *
 * Debounce, stale-search cancellation, and Iranian phone-number normalization are unchanged from
 * before this retarget — only the data-access/model calls changed.
 *
 * **Manager Salon Access Alignment (preserved):** salon *reachability* is still resolved through
 * [CurrentUserIdentityContextRepository]/[availableSalons] — the same ownership-OR-membership-OR-
 * specialist check [ai.rojan.designlab.manager.presentation.dashboard.ManagerDashboardViewModel]
 * uses — so a Manager with only a [ai.rojan.designlab.domain.repository.SalonMembershipAccess] still
 * resolves correctly instead of falling through to a false "no access" state.
 *
 * **Pre-release fix (P1 — search race condition, preserved):** [searchCustomers] cancels the
 * previous [searchJob] before starting a new one — cancellation is cooperative, so a superseded
 * request can never resume and overwrite a newer result. [debounce] adds a ~300ms pause so rapid
 * keystrokes collapse into one real read; [init]/[retry] pass `false` so neither the first paint nor
 * a deliberate retry gets an artificial delay.
 *
 * **Pre-release fix (P1 follow-up — phone-format search miss, preserved):** [query] is run through
 * [ai.rojan.designlab.domain.phone.normalizeIranianPhoneNumber] before reaching the repository, so a
 * manager typing the local `0912...` format still matches a customer stored as `+98912...`.
 *
 * **Phase F2 Timing Fix (initialization-race audit):** [customerRepository] used to be a plain,
 * permanently-captured [CustomerRepository] instance — if this ViewModel was constructed while
 * [ai.rojan.designlab.manager.data.ManagerRepositories.customers] was still the placeholder
 * `EmptyCustomerRepository` (a real, reproducible race: Dashboard's Quick Actions are clickable
 * before `ManagerRepositories.initialize()`'s multi-call backend sync completes), it stayed bound to
 * that empty instance forever — [retry] re-ran against the same stale reference and could never
 * recover. `customerRepository` is now `() -> CustomerRepository`, resolved fresh at the start of
 * each [searchCustomers] call (not cached across calls), so a `retry()` after `initialize()` finally
 * replaces the singleton picks up the real, synced repository without leaving and re-entering the
 * screen. Nothing else changed — debounce, cancellation, phone normalization, and salon-access
 * resolution are byte-for-byte the same logic as before this fix.
 */
class ManagerCustomersViewModel(
    private val customerRepositoryProvider: () -> CustomerRepository,
    private val currentUserIdentityContextRepository: CurrentUserIdentityContextRepository,
) : ViewModel() {

    var state by mutableStateOf<UiState<List<ManagerCustomer>>>(UiState.Loading)
        private set

    /** True only while [state] is [UiState.Empty] because no salon is reachable through this account's real access at all - never because a search genuinely matched nothing. */
    var noAccessibleSalon by mutableStateOf(false)
        private set

    /** Cached once a real salon is confirmed reachable, mirroring the old cached-`salonId` behavior - avoids re-resolving identity context on every keystroke. Left `null` on failure so the next attempt retries. */
    private var hasAccessibleSalon: Boolean? = null
    private var lastQuery: String = ""
    private var searchJob: Job? = null

    init {
        searchCustomers("", debounce = false)
    }

    /** [query] blank/empty is a valid search — the salon's whole customer roster, same convention as [CustomerRepository.search]. */
    fun searchCustomers(query: String, debounce: Boolean = true) {
        lastQuery = query
        state = UiState.Loading
        noAccessibleSalon = false
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            if (!resolveSalonAccess()) return@launch
            // Resolved now, not cached from construction - picks up a real repository that
            // ManagerRepositories.initialize() may have only just finished swapping in.
            val customers = customerRepositoryProvider().search(normalizeIranianPhoneNumber(query))
            state = if (customers.isEmpty()) UiState.Empty else UiState.Success(customers)
        }
    }

    fun retry() = searchCustomers(lastQuery, debounce = false)

    /** Resolves (and caches) whether this account has any real salon access at all. Returns `false` after already setting [state]/[noAccessibleSalon] appropriately - callers should `return@launch` immediately in that case. */
    private suspend fun resolveSalonAccess(): Boolean {
        hasAccessibleSalon?.let { return it }
        return currentUserIdentityContextRepository.getCurrentUserIdentityContext().fold(
            onSuccess = { context ->
                val hasSalon = context.availableSalons().isNotEmpty()
                hasAccessibleSalon = hasSalon
                if (!hasSalon) {
                    noAccessibleSalon = true
                    state = UiState.Empty
                }
                hasSalon
            },
            onFailure = { error ->
                state = UiState.Error(userMessageFor(error))
                false
            },
        )
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
