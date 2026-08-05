package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire-format shape of `ROJAN_Backend`'s `CustomerStatus` (`domain/customer/Customer.kt`) — a real CRM lifecycle, richer than the mobile app's own 4-value `CustomerTag`. See `BackendCustomerRepository`'s mapping for how these are reconciled. */
@Serializable
enum class NetworkCustomerStatus {
    LEAD,
    PROSPECT,
    ACTIVE,
    VIP,
    INACTIVE,
    CHURNED,
}

/** `CreateCustomerRequest` (backend) — email/phoneNumber both nullable, but the domain requires at least one (enforced server-side, not a Bean Validation rule; a request with neither returns a real validation error, not silently accepted). */
@Serializable
data class CreateCustomerRequestDto(
    val fullName: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val company: String? = null,
)

/** `UpdateCustomerRequest` (backend) — every field optional, absent means "leave unchanged" (PATCH merge semantics, not a full replace). */
@Serializable
data class UpdateCustomerRequestDto(
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val company: String? = null,
    val status: NetworkCustomerStatus? = null,
)

@Serializable
data class CustomerResponseDto(
    val id: String,
    val salonId: String,
    /** Linked backend account, if any — null for a walk-in customer with no app account. */
    val userId: String? = null,
    val fullName: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val company: String? = null,
    val status: NetworkCustomerStatus,
    /** Sum of completed-booking service prices, computed server-side — a monetary total, not a 0-100 loyalty score; the mobile domain model's `loyaltyScore` field has no real backend equivalent (see `BackendCustomerRepository`). */
    val lifetimeValue: Double,
    val tags: List<String>,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
)
