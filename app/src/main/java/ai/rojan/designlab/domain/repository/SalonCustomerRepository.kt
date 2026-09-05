package ai.rojan.designlab.domain.repository

/** A real backend customer account who has booked with a specific salon before. */
data class SalonCustomer(
    val id: String,
    val email: String,
    val fullName: String,
)

/**
 * Manager Booking Creation Integrity follow-up. Talks to the ROJAN
 * backend's salon-scoped customer search (`GET /salons/{salonId}/customers`)
 * — the salon owner's own customer roster, never a global user directory.
 * Backend-enforced, not just client-side: only the salon's owner may call
 * this (403 otherwise).
 */
interface SalonCustomerRepository {
    /** [query], when non-null/blank, matches a case-insensitive substring of the customer's name or email — same convention as [SalonRepository.browseSalons]'s `nameFilter`. */
    suspend fun searchCustomers(salonId: String, query: String? = null): Result<List<SalonCustomer>>
}
