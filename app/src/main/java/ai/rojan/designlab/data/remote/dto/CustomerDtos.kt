package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * FIX-006. Wire shapes for the backend Customer CRM
 * (`GET /api/v1/salons/{salonId}/customer-records` and its sub-resources).
 * Only the fields the Manager Customer Profile screen renders are declared;
 * the Retrofit converter is configured with `ignoreUnknownKeys = true`, so
 * `salonId` / `company` / `lifetimeValue` / `active` / timestamps are
 * intentionally omitted.
 */
@Serializable
data class CustomerRecordDto(
    val id: String,
    /** Linked backend account, if any. Used to match a legacy `GET /customers` roster id to this CRM record. */
    val userId: String? = null,
    val fullName: String,
    val phoneNumber: String? = null,
    /** `LEAD | PROSPECT | ACTIVE | VIP | INACTIVE | CHURNED`. */
    val status: String,
    val tags: List<String> = emptyList(),
)

@Serializable
data class CustomerNoteDto(
    val id: String,
    val authorId: String,
    val text: String,
    val createdAt: String,
)

@Serializable
data class CustomerTagDto(
    val id: String,
    val label: String,
    val createdAt: String,
)

/** `type` is one of `STATUS_CHANGED | TAG_ADDED | TAG_REMOVED | NOTE | BOOKING_CREATED | BOOKING_CONFIRMED | BOOKING_COMPLETED | BOOKING_CANCELLED`. */
@Serializable
data class CustomerTimelineEntryDto(
    val type: String,
    val description: String,
    val occurredAt: String,
)
