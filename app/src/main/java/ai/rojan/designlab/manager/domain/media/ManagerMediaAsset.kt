package ai.rojan.designlab.manager.domain.media

/**
 * Central Salon Management — Salon Media UI. Mirrors the backend's real
 * `MediaAsset`/`MediaType` shape exactly (`ROJAN_Backend/domain/.../media/MediaAsset.kt`)
 * rather than inventing a Manager-only structure: this is a thin domain
 * read of the same `media_assets` row every future Customer App/Reception
 * App/Website consumer will read too. `DOCUMENT` exists on the backend
 * enum but belongs to a different, unrelated feature (Document Archive's
 * own Security Gate), not Salon Media, so it's not represented here.
 * [SPECIALIST_PHOTO] (Media Sprint P0): uploaded through this exact same
 * `POST /salons/{salonId}/media` pipeline, then its returned URL is
 * written onto `Specialist.photoUrl` via the existing specialist update
 * endpoint — no parallel upload mechanism, no schema change. [PORTFOLIO]/
 * [SERVICE_IMAGE] (Media System Evolution v2): real, [targetId]-scoped
 * types — many rows per specialist/service, unlike the single-URL
 * `SPECIALIST_PHOTO` field, so these are read/managed as a list scoped by
 * `targetId`, never written onto another entity's field.
 */
enum class ManagerMediaType { LOGO, COVER, GALLERY, SPECIALIST_PHOTO, PORTFOLIO, SERVICE_IMAGE }

/**
 * [id]/[url] are the two fields every other consumer of this same backend
 * row will also need — nothing Manager-specific added. [targetId]/
 * [displayOrder] (Media System Evolution v2): real backend fields since
 * that evolution - `targetId` is `null` for every type except
 * [ManagerMediaType.PORTFOLIO]/[ManagerMediaType.SERVICE_IMAGE].
 */
data class ManagerMediaAsset(
    val id: String,
    val mediaType: ManagerMediaType,
    val url: String,
    val originalName: String,
    val createdAt: String,
    val targetId: String? = null,
    val displayOrder: Int = 0,
)

/** The two identity slots a `MediaAsset` can be assigned into on the salon itself (`Salon.logoMediaId`/`.coverMediaId`) — distinct from [ManagerMediaType.GALLERY], which has no slot concept. */
enum class ManagerIdentitySlot { LOGO, COVER }
