package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
enum class NetworkBookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
}

@Serializable
data class CreateBookingRequestDto(
    val salonId: String,
    val serviceId: String,
    val specialistId: String,
    /** ISO-8601 local date-time, e.g. `"2026-09-01T10:00:00"` — one of the windows returned by the availability endpoint. */
    val startTime: String,
    val notes: String? = null,
)

/**
 * Request body for `POST /api/v1/salons/{salonId}/bookings` — the owner-authorized counterpart to
 * [CreateBookingRequestDto], which always books the caller themself and has no `customerId` field
 * at all. [customerId] is a CRM `Customer` id (from `GET /salons/{salonId}/customers`), not a
 * `User` id — the target customer must already be linked to an account (backend returns 409
 * otherwise; see `ManagerBookingApi.createForCustomer`'s doc comment).
 */
@Serializable
data class CreateBookingForCustomerRequestDto(
    val customerId: String,
    val serviceId: String,
    val specialistId: String,
    /** ISO-8601 local date-time, e.g. `"2026-09-01T10:00:00"` — one of the windows returned by the availability endpoint. */
    val startTime: String,
    val notes: String? = null,
)

@Serializable
data class RescheduleBookingRequestDto(
    /** ISO-8601 local date-time, e.g. `"2026-09-02T14:00:00"` — one of the windows returned by the availability endpoint. */
    val newStartTime: String,
)

@Serializable
data class ServiceSummaryDto(val id: String, val name: String)

@Serializable
data class SpecialistSummaryDto(val id: String, val name: String)

@Serializable
data class CustomerSummaryDto(val id: String, val name: String, val phone: String? = null)

/**
 * [service]/[specialist]/[customer] are `ROJAN_System1_Backend_Decision_v2.md`
 * §3's approved enrichment — additive, alongside the unchanged flat
 * [serviceId]/[specialistId]/[customerId]. All three default to `null`
 * because the backend has not shipped this enrichment yet (verified
 * against `origin/feature/auth-rate-limit-finalization`); every real
 * response today omits them, and deserialization must not fail on their
 * absence. Do not synthesize a value client-side when these are `null`.
 */
@Serializable
data class BookingResponseDto(
    val id: String,
    val salonId: String,
    val serviceId: String,
    val specialistId: String,
    val customerId: String,
    val startTime: String,
    val endTime: String,
    val status: NetworkBookingStatus,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val service: ServiceSummaryDto? = null,
    val specialist: SpecialistSummaryDto? = null,
    val customer: CustomerSummaryDto? = null,
)
