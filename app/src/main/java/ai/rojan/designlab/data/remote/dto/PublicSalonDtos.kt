package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Deliberately separate from the authenticated Salon/Specialist DTOs —
 * mirrors `ROJAN_Backend`'s own `PublicSalonResponse` family, which
 * carries no `ownerId`/`userId` linkage fields since any unauthenticated
 * caller can request these.
 *
 * Data Parity Audit: [coverImageUrl] is a real field on the backend's
 * `PublicSalonResponse` that this DTO never captured before (confirmed on
 * the live wire — a distinct media asset from any gallery photo, backend's
 * `spring.jackson.default-property-inclusion: non_null` omits the key
 * entirely when unset, same as every other nullable field here).
 *
 * Every nullable field now has a `null` default (Salon Guest Detail
 * MalformedResponseException investigation): a nullable Kotlin type alone
 * does not make a kotlinx.serialization key optional-on-absence, only an
 * explicit default does — without one, a salon missing any of these (e.g.
 * no coordinates set) 404s the whole guest detail screen with a decode
 * crash instead of just rendering that field as absent.
 */
@Serializable
data class PublicSalonResponseDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val phone: String,
    val address: String,
    val logoUrl: String? = null,
    val coverImageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
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

// Data Parity Audit / Guest Detail MalformedResponseException investigation:
// `= null` on every nullable field below, same reasoning as
// PublicSalonResponseDto's own doc comment above.

@Serializable
data class PublicServiceCategoryResponseDto(
    val id: String,
    val name: String,
    val description: String? = null,
)

@Serializable
data class PublicServiceResponseDto(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String? = null,
    val durationMinutes: Int,
    val price: Double,
)

@Serializable
data class PublicSpecialistResponseDto(
    val id: String,
    val displayName: String,
    val bio: String? = null,
    val photoUrl: String? = null,
)

/** Mirrors `ROJAN_Backend`'s `PublicMediaAssetResponse` (`PublicSalonController.gallery`) — real, publicly-servable GALLERY/PORTFOLIO images only, server-side filtered. */
@Serializable
data class PublicMediaAssetResponseDto(
    val id: String,
    val mediaType: String,
    val url: String,
)
