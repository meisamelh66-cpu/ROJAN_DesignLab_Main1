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
    /** Manager Booking Creation Integrity follow-up: set only when a salon owner is booking on behalf of a customer returned by `GET /salons/{salonId}/customers`. Omitted for the normal self-booking case. */
    val customerId: String? = null,
)

/** TEAM2-003: request body for `PUT /api/v1/bookings/{id}/reschedule` — mirrors the backend's `RescheduleBookingRequest`. */
@Serializable
data class RescheduleBookingRequestDto(
    /** ISO-8601 local date-time, same format/source as [CreateBookingRequestDto.startTime]. */
    val newStartTime: String,
)

/**
 * Request body for `POST /api/v1/salons/{salonId}/bookings` — the owner-authorized counterpart to
 * [CreateBookingRequestDto], which always books the caller themself and has no `customerId` field
 * at all. [customerId] is a CRM `Customer` id (from `GET /salons/{salonId}/customers`), not a
 * `User` id. TASK-003 (Walk-in Customer Booking): the target customer no longer needs a linked
 * account — a customer with none becomes a real walk-in booking (response `customerId` null,
 * `crmCustomerId` set), never a 409 any more (see `ManagerBookingApi.createForCustomer`'s doc
 * comment).
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
 *
 * TASK-003 (Walk-in Customer Booking): [customerId] became nullable the
 * moment the real backend started supporting walk-in bookings (a booking
 * for a CRM customer with no linked account) — exactly one of [customerId]/
 * [crmCustomerId] is ever non-null, never both, never neither. This was a
 * required fix, not a defensive one: kotlinx.serialization throws on an
 * explicit JSON `null` for a non-nullable property with no default, so the
 * previous non-nullable [customerId] would have crashed deserializing any
 * booking list/detail response that happened to contain a walk-in booking
 * (reachable today via `ManagerBookingApi`/`SalonBookingApi` — Manager's and
 * Reception's own salon-wide booking views; never via the self-service
 * `BookingApi`, which is scoped to the caller's own linked account and so
 * can never return one).
 */
@Serializable
data class BookingResponseDto(
    val id: String,
    val salonId: String,
    val serviceId: String,
    val specialistId: String,
    val customerId: String?,
    /** TASK-003: the booking's CRM customer id, set only for a walk-in booking with no linked account. `null` whenever [customerId] is set. */
    val crmCustomerId: String? = null,
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
