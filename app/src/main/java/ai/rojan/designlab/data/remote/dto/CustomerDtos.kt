package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire-format CRM status for a customer record.
 *
 * Backend:
 * ROJAN_Backend CustomerStatus.
 * Distinct from booking status and mobile CustomerTag concepts.
 */
@Serializable
enum class NetworkCustomerStatus {
    LEAD,
    PROSPECT,
    ACTIVE,
    VIP,
    INACTIVE,
    CHURNED,
}


/**
 * Request body for POST /api/v1/salons/{salonId}/customers.
 *
 * email/phoneNumber are nullable, but backend requires at least one.
 */
@Serializable
data class CreateCustomerRequestDto(
    val fullName: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val company: String? = null,
)


/**
 * Request body for PATCH /api/v1/salons/{salonId}/customers/{customerId}.
 *
 * Partial update semantics:
 * null means keep existing value.
 */
@Serializable
data class UpdateCustomerRequestDto(
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val company: String? = null,
    val status: NetworkCustomerStatus? = null,
)


/**
 * Backend Customer CRM response.
 */
@Serializable
data class CustomerResponseDto(
    val id: String,
    val salonId: String,

    /**
     * Linked backend account, if any.
     * Null for walk-in customers without app account.
     */
    val userId: String? = null,

    val fullName: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val company: String? = null,

    val status: NetworkCustomerStatus,

    /**
     * Sum of completed booking service prices.
     * Computed server-side.
     */
    val lifetimeValue: Double,

    val tags: List<String> = emptyList(),

    val active: Boolean,

    val createdAt: String,
    val updatedAt: String,
)


/**
 * Customer CRM note response.
 */
@Serializable
data class CustomerNoteResponseDto(
    val id: String,
    val authorId: String,
    val text: String,
    val createdAt: String,
)