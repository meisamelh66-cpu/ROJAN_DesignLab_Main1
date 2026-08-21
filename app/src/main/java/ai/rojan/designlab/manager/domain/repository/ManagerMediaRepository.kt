package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.dashboard.ManagerSalonSummary
import ai.rojan.designlab.manager.domain.media.ManagerIdentitySlot
import ai.rojan.designlab.manager.domain.media.ManagerMediaAsset
import ai.rojan.designlab.manager.domain.media.ManagerMediaType

/**
 * Central Salon Management — Salon Media UI, a management layer over the
 * existing Media Foundation (`POST`/`GET`/`DELETE /api/v1/salons/{salonId}/media`,
 * `PATCH .../media/reorder`, `PUT /api/v1/salons/{salonId}/identity-media` —
 * all real, already-shipped backend endpoints, verified directly against
 * `ROJAN_Backend` source). Every reference here is by id/url through those
 * same contracts — no Manager-only media structure, no separate copy of
 * salon media for any other client; a future Customer App/Reception App/
 * Website reads the identical rows this repository writes.
 *
 * [assignIdentity] is a second, separate call from [upload] on purpose,
 * mirroring the backend's own two-step design (`AssignIdentityMediaUseCase`
 * is independent of `UploadMediaUseCase`) — an uploaded asset does not
 * become the salon's active logo/cover until explicitly assigned. The
 * ViewModel composes both calls into one user-facing action for
 * logo/cover; gallery images only ever call [upload]/[delete]/[reorder],
 * since [ManagerMediaType.GALLERY] has no identity slot.
 *
 * [targetId] on [list]/[upload], and [reorder] (Media System Evolution v2):
 * `targetId` narrows to one specialist's [ManagerMediaType.PORTFOLIO] or
 * one service's [ManagerMediaType.SERVICE_IMAGE] set - omit it for
 * salon-flat types, exactly like the backend's own optional query
 * param/form field.
 */
interface ManagerMediaRepository {

    suspend fun list(salonId: String, mediaType: ManagerMediaType? = null, targetId: String? = null): Result<List<ManagerMediaAsset>>

    suspend fun upload(
        salonId: String,
        mediaType: ManagerMediaType,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        targetId: String? = null,
    ): Result<ManagerMediaAsset>

    suspend fun delete(salonId: String, mediaId: String): Result<Unit>

    /** [mediaId] `null` clears the slot (backend semantics, `AssignIdentityMediaRequest`'s own doc comment). */
    suspend fun assignIdentity(salonId: String, slot: ManagerIdentitySlot, mediaId: String?): Result<ManagerSalonSummary>

    /** [mediaIds] must be exactly the current (mediaType, targetId) group's members, just permuted - the backend rejects (and this call fails) a partial or foreign list outright rather than partially applying it. */
    suspend fun reorder(salonId: String, mediaType: ManagerMediaType, targetId: String?, mediaIds: List<String>): Result<Unit>
}
