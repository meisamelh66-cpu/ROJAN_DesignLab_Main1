package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.dashboard.ManagerSalonSummary
import ai.rojan.designlab.manager.domain.media.ManagerIdentitySlot
import ai.rojan.designlab.manager.domain.media.ManagerMediaAsset
import ai.rojan.designlab.manager.domain.media.ManagerMediaType

/**
 * Central Salon Management — Salon Media UI, a management layer over the
 * existing Media Foundation (`POST`/`GET`/`DELETE /api/v1/salons/{salonId}/media`,
 * `PUT /api/v1/salons/{salonId}/identity-media` — all real, already-shipped
 * backend endpoints, verified directly against `ROJAN_Backend` source).
 * Every reference here is by id/url through those same contracts — no
 * Manager-only media structure, no separate copy of salon media for any
 * other client; a future Customer App/Reception App/Website reads the
 * identical `media_assets`/`salons.logo_media_id`/`.cover_media_id` rows
 * this repository writes.
 *
 * [assignIdentity] is a second, separate call from [upload] on purpose,
 * mirroring the backend's own two-step design (`AssignIdentityMediaUseCase`
 * is independent of `UploadMediaUseCase`) — an uploaded asset does not
 * become the salon's active logo/cover until explicitly assigned. The
 * ViewModel composes both calls into one user-facing action for
 * logo/cover; gallery images only ever call [upload]/[delete], since
 * [ManagerMediaType.GALLERY] has no identity slot.
 */
interface ManagerMediaRepository {

    suspend fun list(salonId: String, mediaType: ManagerMediaType? = null): Result<List<ManagerMediaAsset>>

    suspend fun upload(
        salonId: String,
        mediaType: ManagerMediaType,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): Result<ManagerMediaAsset>

    suspend fun delete(salonId: String, mediaId: String): Result<Unit>

    /** [mediaId] `null` clears the slot (backend semantics, `AssignIdentityMediaRequest`'s own doc comment). */
    suspend fun assignIdentity(salonId: String, slot: ManagerIdentitySlot, mediaId: String?): Result<ManagerSalonSummary>
}
