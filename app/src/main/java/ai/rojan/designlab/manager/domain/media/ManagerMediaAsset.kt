package ai.rojan.designlab.manager.domain.media

/**
 * Central Salon Management — Salon Media UI. Mirrors the backend's real
 * `MediaAsset`/`MediaType` shape exactly (`ROJAN_Backend/domain/.../media/MediaAsset.kt`)
 * rather than inventing a Manager-only structure: this is a thin domain
 * read of the same `media_assets` row every future Customer App/Reception
 * App/Website consumer will read too. Only the three types this screen
 * manages are represented here — `PORTFOLIO`/`DOCUMENT` exist on the
 * backend enum but belong to other, unrelated features (Document Archive's
 * own Security Gate in particular), not Salon Media.
 */
enum class ManagerMediaType { LOGO, COVER, GALLERY }

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
