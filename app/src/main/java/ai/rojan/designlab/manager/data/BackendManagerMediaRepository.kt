package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerMediaApi
import ai.rojan.designlab.data.remote.ManagerSalonApi
import ai.rojan.designlab.data.remote.dto.AssignIdentityMediaRequestDto
import ai.rojan.designlab.data.remote.dto.MediaAssetResponseDto
import ai.rojan.designlab.data.remote.dto.SalonResponseDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.dashboard.ManagerSalonSummary
import ai.rojan.designlab.manager.domain.media.ManagerIdentitySlot
import ai.rojan.designlab.manager.domain.media.ManagerMediaAsset
import ai.rojan.designlab.manager.domain.media.ManagerMediaType
import ai.rojan.designlab.manager.domain.repository.ManagerMediaRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Real backend-backed [ManagerMediaRepository] (Central Salon Management —
 * Salon Media UI). No cache, same "single-screen consumer, no list to keep
 * in sync elsewhere" reasoning as [BackendManagerSalonRepository].
 */
class BackendManagerMediaRepository(
    private val managerMediaApi: ManagerMediaApi,
    private val managerSalonApi: ManagerSalonApi,
) : ManagerMediaRepository {

    override suspend fun list(salonId: String, mediaType: ManagerMediaType?): Result<List<ManagerMediaAsset>> =
        safeApiCall { managerMediaApi.list(salonId, mediaType?.name) }
            .map { assets -> assets.map { it.toDomain() } }

    override suspend fun upload(
        salonId: String,
        mediaType: ManagerMediaType,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): Result<ManagerMediaAsset> =
        safeApiCall {
            val filePart = MultipartBody.Part.createFormData(
                name = "file",
                filename = fileName,
                body = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull()),
            )
            val mediaTypePart = mediaType.name.toRequestBody("text/plain".toMediaTypeOrNull())
            managerMediaApi.upload(salonId, filePart, mediaTypePart)
        }.map { it.toDomain() }

    override suspend fun delete(salonId: String, mediaId: String): Result<Unit> =
        safeApiCall { managerMediaApi.delete(salonId, mediaId) }

    override suspend fun assignIdentity(salonId: String, slot: ManagerIdentitySlot, mediaId: String?): Result<ManagerSalonSummary> =
        safeApiCall {
            managerSalonApi.assignIdentityMedia(salonId, AssignIdentityMediaRequestDto(slot = slot.name, mediaId = mediaId))
        }.map { it.toDomain() }

    private fun MediaAssetResponseDto.toDomain() = ManagerMediaAsset(
        id = id,
        mediaType = ManagerMediaType.valueOf(mediaType),
        url = url,
        originalName = originalName,
        createdAt = createdAt,
    )

    private fun SalonResponseDto.toDomain() = ManagerSalonSummary(
        id = id,
        name = name,
        description = description,
        phone = phone,
        email = email,
        address = address,
        latitude = latitude,
        longitude = longitude,
        active = active,
        logoUrl = logoUrl,
        coverImageUrl = coverImageUrl,
    )
}
