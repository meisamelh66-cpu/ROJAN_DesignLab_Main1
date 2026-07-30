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
