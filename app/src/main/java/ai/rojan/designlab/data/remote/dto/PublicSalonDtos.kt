package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Deliberately separate from the authenticated Salon/Specialist DTOs —
 * mirrors `ROJAN_Backend`'s own `PublicSalonResponse` family, which
 * carries no `ownerId`/`userId` linkage fields since any unauthenticated
 * caller can request these.
 */
@Serializable
data class PublicSalonResponseDto(
    val id: String,
    val name: String,
    val description: String?,
    val phone: String,
    val address: String,
    val logoUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
)

/**
 * One row of the unauthenticated salon directory (`GET /api/v1/public/salons`),
 * mirroring `ROJAN_Backend`'s `PublicSalonListResponse` — the marketplace
 * directory row, deliberately narrower than the by-slug [PublicSalonResponseDto]:
 * the backend keeps `phone`/`address`/`description` behind the by-slug endpoint
 * and this row carries only `id`/`slug`/`name` plus optional imagery (verified
 * against the live response, 2026-09-10). **Only [id] and [name] are ever
 * guaranteed present** — every other field is nullable so a lean row can never
 * throw `MissingFieldException` (that crash was caught on-device). Maps to the
 * [ai.rojan.designlab.domain.repository.Salon] discovery shape.
 */
@Serializable
data class PublicSalonSummaryResponseDto(
    val id: String,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val logoUrl: String? = null,
    val coverUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceKm: Double? = null,
)

@Serializable
data class PublicServiceCategoryResponseDto(
    val id: String,
    val name: String,
    val description: String?,
)

@Serializable
data class PublicServiceResponseDto(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String?,
    val durationMinutes: Int,
    val price: Double,
)

@Serializable
data class PublicSpecialistResponseDto(
    val id: String,
    val displayName: String,
    val bio: String?,
    val photoUrl: String?,
)
