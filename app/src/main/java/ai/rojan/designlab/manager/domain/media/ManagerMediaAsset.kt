package ai.rojan.designlab.manager.domain.media

/**
 * Central Salon Management — Salon Media UI, plus Specialist Profile
 * Expansion's [SPECIALIST_PHOTO]. Mirrors the backend's real
 * `MediaAsset`/`MediaType` shape exactly (`ROJAN_Backend/domain/.../media/MediaAsset.kt`)
 * rather than inventing a Manager-only structure: this is a thin domain
 * read of the same `media_assets` row every future Customer App/Reception
 * App/Website consumer will read too. [SPECIALIST_PHOTO] deliberately
 * reuses this same salon-scoped upload pipeline (`ManagerMediaApi.upload`)
 * rather than a parallel one - per the backend's own doc comment, the
 * association to *which* specialist owns an upload is carried by
 * `Specialist.photoUrl` (set via the existing specialist update endpoint
 * after upload), not a `targetId` on the media row (unlike `PORTFOLIO`,
 * which `DOCUMENT`/other target-required types do need - neither is
 * represented here since they belong to other, unrelated features).
 */
enum class ManagerMediaType { LOGO, COVER, GALLERY, SPECIALIST_PHOTO }

/** [id]/[url] are the two fields every other consumer of this same backend row will also need — nothing Manager-specific added. */
data class ManagerMediaAsset(
    val id: String,
    val mediaType: ManagerMediaType,
    val url: String,
    val originalName: String,
    val createdAt: String,
)

/** The two identity slots a `MediaAsset` can be assigned into on the salon itself (`Salon.logoMediaId`/`.coverMediaId`) — distinct from [ManagerMediaType.GALLERY], which has no slot concept. */
enum class ManagerIdentitySlot { LOGO, COVER }
