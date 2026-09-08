package ai.rojan.designlab.manager.domain.customer

/**
 * FIX-006. The Manager Customer Profile screen's data, assembled from the
 * backend Customer CRM (`GET /customer-records/{id}` + `/notes` + `/tags` +
 * `/timeline` + `/bookings`). Replaces the in-memory `ManagerCustomer` /
 * `InMemoryCustomerRepository` sample record the screen read before.
 */
data class ManagerCustomerProfile(
    val fullName: String,
    val phone: String,
    /** Localized customer status, with any backend tags appended. */
    val statusLabel: String,
    /** Total bookings this customer has with the salon (`/bookings` totalElements). */
    val totalVisits: Int,
    /** Booking lifecycle entries from the CRM timeline, newest first. */
    val history: List<CustomerServiceHistoryEntry>,
    /** Manager notes joined newest-first, or `null` when there are none. */
    val notes: String?,
)

/**
 * One row of the profile's "سابقه خدمات" section. Carried over from the
 * previous in-memory model so the screen's section layout is unchanged;
 * [specialist] and [price] are blank because the backend booking/timeline
 * payloads do not carry a specialist name or a price (contract unchanged).
 */
data class CustomerServiceHistoryEntry(
    val date: String,
    val service: String,
    val specialist: String,
    val price: String,
)
