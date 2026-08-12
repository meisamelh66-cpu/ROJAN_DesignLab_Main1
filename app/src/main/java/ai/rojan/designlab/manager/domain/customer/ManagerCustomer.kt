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
 * (the single latest one). Read-only: the backend has no note-creation
 * endpoint, so nothing here writes a new one. [authorId] is deliberately
 * not carried through - nothing in this app resolves a `userId` to a
 * display name, and every note visible to a Manager account was written
 * by a manager/owner of this salon, so a fabricated "author" label would
 * add nothing real.
 */
data class CustomerNote(
    val id: String,
    val text: String,
    val createdAt: String,
)
