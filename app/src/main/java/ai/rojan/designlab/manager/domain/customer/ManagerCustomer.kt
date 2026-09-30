package ai.rojan.designlab.manager.domain.customer

/**
 * Manager Domain Foundation Phase 1 — replaces the screen-local
 * `ManagerCustomer` previously defined in
 * `ai.rojan.designlab.manager.screens.customers.ManagerCustomerSampleData`.
 * [lastVisit]/[totalVisits] are carried over from that old model (the
 * Customers screens render them) even though they weren't in the new
 * field spec, so no existing display breaks; [tag]/[loyaltyScore]/
 * [notes] are the new structured fields.
 */
data class ManagerCustomer(
    val id: String,
    val name: String,
    val phone: String,
    val tag: CustomerTag,
    val loyaltyScore: Int,
    val notes: String?,
    val lastVisit: String,
    val totalVisits: Int,
    /**
     * CRM Customer -> User Account Linking, Phase 2: mirrors the backend's
     * `CustomerResponse.userId` exactly - null for an unlinked walk-in, a real backend account id
     * once linked. The backend remains the sole source of truth for this value; nothing in this app
     * ever sets it except by mapping a real `CustomerResponseDto`/`UserLinkCandidateResponseDto`-
     * derived response (see [ai.rojan.designlab.manager.data.BackendCustomerRepository]). Defaults to
     * `null` so the one existing positional-argument construction site keeps compiling unchanged.
     */
    val userId: String? = null,
)

/**
 * CRM Customer -> User Account Linking, Phase 2: the read-only result of
 * `GET .../customers/{customerId}/link/lookup` (`ROJAN_Backend`'s `LookupUserForCustomerLinkUseCase`)
 * - the one User account matching this customer's own already-on-file phone number, shown to a
 * Manager for explicit visual confirmation before [ai.rojan.designlab.manager.domain.repository.CustomerRepository.linkToUser]
 * is ever called. Deliberately as minimal as the backend response itself - no email, role, or other
 * account data.
 */
data class UserLinkCandidate(
    val userId: String,
    val fullName: String,
    val phoneNumber: String,
)

/**
 * Supporting record for [ManagerCustomer]'s service history — not named
 * in the Phase 1 spec's field list, but required by
 * [ai.rojan.designlab.manager.screens.customers.ManagerCustomerProfileScreen],
 * so migrated alongside rather than dropped.
 */
data class CustomerServiceHistoryEntry(
    val date: String,
    val service: String,
    val specialist: String,
    val price: String,
)

/**
 * One manager note on a customer (CRM Foundation, Phase 6 Step 5) — the
 * full history the backend already returns via `GET .../customers/{id}/notes`,
 * previously fetched and immediately truncated to just [ManagerCustomer.notes]
 * (the single latest one).
 *
 * Phase F4 correction: this doc previously claimed the backend has no
 * note-creation endpoint - it does (`POST .../customers/{id}/notes`, see
 * [ai.rojan.designlab.manager.domain.repository.CustomerRepository.createNote]).
 * [authorId] is still deliberately not carried through here - nothing in
 * this app resolves a `userId` to a display name, and every note visible to
 * a Manager account was written by a manager/owner of this salon, so a
 * fabricated "author" label would add nothing real; this is unchanged by
 * Phase F4 and out of its scope.
 */
data class CustomerNote(
    val id: String,
    val text: String,
    val createdAt: String,
)
