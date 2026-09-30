package ai.rojan.designlab.manager.presentation.customers

import ai.rojan.designlab.domain.repository.CurrentUserIdentityContextRepository
import ai.rojan.designlab.domain.repository.availableSalons
import ai.rojan.designlab.manager.domain.customer.CustomerNote
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.customer.UserLinkCandidate
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
 * Phase F4 — the note-creation form's own status, deliberately separate from [ManagerCustomerProfileViewModel.state]:
 * a failed or in-flight note submission must never collapse the whole profile to [UiState.Loading]/[UiState.Error] -
 * [state] keeps showing the already-loaded profile throughout.
 */
sealed interface NoteSubmissionState {
    data object Idle : NoteSubmissionState
    data object Submitting : NoteSubmissionState
    data class Failed(val message: String) : NoteSubmissionState
}

/**
 * CRM Customer -> User Account Linking, Phase 2 — the explicit lookup-then-confirm-then-link flow's
 * own status, independent of [ManagerCustomerProfileViewModel.state] for the exact same reason
 * [NoteSubmissionState] is: an in-flight or failed lookup/link must never collapse the already-loaded
 * profile to [UiState.Loading]/[UiState.Error].
 *
 * [Candidate] is the one state between an explicit [ManagerCustomerProfileViewModel.startLinkLookup]
 * and an explicit [ManagerCustomerProfileViewModel.confirmLink] or
 * [ManagerCustomerProfileViewModel.cancelLinkCandidate] - nothing transitions out of it
 * automatically, which is the whole point: the backend already resolved a real account, but linking
 * it is a separate, Manager-confirmed decision.
 */
sealed interface CustomerLinkState {
    data object Idle : CustomerLinkState
    data object LookingUp : CustomerLinkState
    data class Candidate(val candidate: UserLinkCandidate) : CustomerLinkState
    data object Linking : CustomerLinkState
    data class Failed(val message: String) : CustomerLinkState
}

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
 *
 * **Phase F2 Timing Fix (initialization-race audit):** [customerRepository] used to be a plain,
 * permanently-captured [CustomerRepository] instance, exposing the same stale-capture race as
 * [ManagerCustomersViewModel] (see its own doc comment) — if constructed while
 * [ai.rojan.designlab.manager.data.ManagerRepositories.customers] was still `EmptyCustomerRepository`,
 * [retry] could never recover. `customerRepository` is now `() -> CustomerRepository`, resolved
 * exactly once per [load] call - not once per individual repository method - into a local `val` at
 * the top of the customer-detail work, so [loadDetail]/[getById]/[getServiceHistory]/[getNoteHistory]
 * within one load pass all read the *same* repository instance (avoiding a worse inconsistency: a
 * mid-flight singleton swap making `getById` land on a different instance than the one `loadDetail`
 * had just populated). A subsequent [retry] calls [load] again, re-resolving the provider fresh at
 * that later point. The P0 identity contract is unaffected: [customerId] is still the only value
 * passed to every call on that resolved instance.
 *
 * **Phase F4 (Customer Notes completion):** [submitNote] adds Create to the previously read-only
 * Notes capability, via the backend's real, already-tested `POST .../notes`
 * ([CustomerRepository.createNote]). The backend stays the sole source of truth for note content/
 * order: a successful create does not locally append to [state]'s `notes` - it re-runs [load] (the
 * same full re-fetch [retry] already triggers), so the list [state] ends up showing is always exactly
 * what the backend just returned, never a locally-guessed duplicate. [noteSubmissionState] is
 * intentionally independent of [state]: a failed or in-flight note submission never touches the
 * already-loaded profile - only [UiState.Success] data displayed via [state] can regress to
 * [UiState.Loading]/[UiState.Error], and [submitNote] never does that on failure.
 */
class ManagerCustomerProfileViewModel(
    private val customerRepositoryProvider: () -> CustomerRepository,
    private val currentUserIdentityContextRepository: CurrentUserIdentityContextRepository,
    private val customerId: String,
) : ViewModel() {

    var state by mutableStateOf<UiState<ManagerCustomerProfileData>>(UiState.Loading)
        private set

    /** True only while [state] is [UiState.Empty] because no salon is reachable through this account's real access at all - never on a genuine customer-not-found. */
    var noAccessibleSalon by mutableStateOf(false)
        private set

    /** Phase F4 — see this class's own doc comment. Independent of [state]; a failed/in-flight note submission never touches it. */
    var noteSubmissionState by mutableStateOf<NoteSubmissionState>(NoteSubmissionState.Idle)
        private set

    /** CRM Customer -> User Account Linking, Phase 2 — see [CustomerLinkState]'s own doc comment. Independent of [state]. */
    var linkState by mutableStateOf<CustomerLinkState>(CustomerLinkState.Idle)
        private set

    init {
        load()
    }

    fun retry() = load()

    /**
     * CRM Customer -> User Account Linking, Phase 2 — explicit, Manager-initiated lookup only; never
     * called automatically. On success, [linkState] becomes [CustomerLinkState.Candidate] and stays
     * there until the Manager explicitly calls [confirmLink] or [cancelLinkCandidate] - nothing here
     * links anything.
     */
    fun startLinkLookup() {
        linkState = CustomerLinkState.LookingUp
        viewModelScope.launch {
            customerRepositoryProvider().lookupUserForLink(customerId)
                .onSuccess { candidate -> linkState = CustomerLinkState.Candidate(candidate) }
                .onFailure { error -> linkState = CustomerLinkState.Failed(userMessageFor(error)) }
        }
    }

    /** Discards a [CustomerLinkState.Candidate]/[CustomerLinkState.Failed] without linking anything - the Manager declined. */
    fun cancelLinkCandidate() {
        linkState = CustomerLinkState.Idle
    }

    /**
     * CRM Customer -> User Account Linking, Phase 2 — the only path that can call
     * [CustomerRepository.linkToUser], and only ever with a [userId] the Manager has already seen and
     * explicitly confirmed (normally [CustomerLinkState.Candidate.candidate]'s own id - the UI never
     * has any other source to pass here). On success, [state] is updated directly from the backend's
     * own returned [ManagerCustomer] (the `POST .../link` response is already the full, authoritative
     * post-link customer - no extra fetch needed, and never a locally fabricated `userId`); history/
     * notes already in [state] are preserved unchanged. A failure leaves [state] completely untouched
     * and reports the real backend error via [linkState].
     */
    fun confirmLink(userId: String) {
        linkState = CustomerLinkState.Linking
        viewModelScope.launch {
            customerRepositoryProvider().linkToUser(customerId, userId)
                .onSuccess { linkedCustomer ->
                    linkState = CustomerLinkState.Idle
                    val current = state
                    if (current is UiState.Success) {
                        state = UiState.Success(current.data.copy(customer = linkedCustomer))
                    } else {
                        // Unreachable in practice (confirmLink is only ever invoked from a Success-state
                        // UI), but if state somehow isn't Success, a full re-fetch is the honest fallback
                        // rather than fabricating history/notes we don't have.
                        load()
                    }
                }
                .onFailure { error -> linkState = CustomerLinkState.Failed(userMessageFor(error)) }
        }
    }

    /**
     * Phase F4. Client-side mirror of the backend's own `@NotBlank`/`@Size(max = 2000)` validation
     * (`AddCustomerNoteRequest`) - rejected locally without a network round trip; the backend still
     * re-validates independently, this is a UX shortcut, not a replacement for it. On success, re-runs
     * [load] rather than locally appending, so [state]'s notes always come straight from the backend
     * (see this class's own doc comment on why).
     */
    fun submitNote(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            noteSubmissionState = NoteSubmissionState.Failed("متن یادداشت نمی‌تواند خالی باشد.")
            return
        }
        if (trimmed.length > MAX_NOTE_TEXT_LENGTH) {
            noteSubmissionState = NoteSubmissionState.Failed("متن یادداشت نمی‌تواند بیش از ۲۰۰۰ کاراکتر باشد.")
            return
        }

        noteSubmissionState = NoteSubmissionState.Submitting
        viewModelScope.launch {
            customerRepositoryProvider().createNote(customerId, trimmed)
                .onSuccess {
                    noteSubmissionState = NoteSubmissionState.Idle
                    load()
                }
                .onFailure { error ->
                    noteSubmissionState = NoteSubmissionState.Failed(userMessageFor(error))
                }
        }
    }

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

            // Resolved once for this whole load pass (not per call, and not cached from
            // construction) - picks up a real repository ManagerRepositories.initialize() may have
            // only just finished swapping in, while keeping loadDetail/getById/getServiceHistory/
            // getNoteHistory internally consistent against the one instance loadDetail populated.
            val repository = customerRepositoryProvider()

            repository.loadDetail(customerId).onFailure { error ->
                state = UiState.Error(userMessageFor(error))
                return@launch
            }

            // customerId is the only identity source below - never a substitute customer.
            val customer = repository.getById(customerId)
            if (customer == null) {
                state = UiState.Empty
                return@launch
            }

            state = UiState.Success(
                ManagerCustomerProfileData(
                    customer = customer,
                    history = repository.getServiceHistory(customerId),
                    notes = repository.getNoteHistory(customerId),
                ),
            )
        }
    }

    private companion object {
        /** Mirrors the backend's `AddCustomerNoteRequest`'s `@Size(max = 2000)` (Phase F4) - a UX shortcut, not a replacement for that server-side check. */
        const val MAX_NOTE_TEXT_LENGTH = 2000
    }
}
