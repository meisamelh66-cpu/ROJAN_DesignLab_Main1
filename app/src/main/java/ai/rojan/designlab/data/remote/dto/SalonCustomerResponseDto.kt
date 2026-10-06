package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of one entry in `GET /api/v1/salons/{salonId}/customers`'s
 * `content` array (`ROJAN_Backend`'s `CustomerController.list`/`CustomerResponse`).
 * Only the fields this app actually consumes are declared — the backend
 * also sends `salonId`/`userId`/`status`/`lifetimeValue`/`company`/`active`/
 * `createdAt`/`updatedAt`, silently ignored here.
 */
@Serializable
data class SalonCustomerResponseDto(
    val id: String,
    val fullName: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val tags: List<String> = emptyList(),
)
