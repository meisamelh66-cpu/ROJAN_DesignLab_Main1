package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire-format DTO for the ROJAN backend's `MediaAssetResponse` — see
 * `ROJAN_Backend/api/.../media/MediaDtos.kt`. [url] is always a resolved,
 * ready-to-render address (public CDN URL for LOGO/COVER/GALLERY today —
 * never a signed document link, that's Document Archive's separate,
 * permission-gated `access-url` endpoint). [targetId]/[displayOrder]
 * (Media System Evolution v2): real backend fields since that evolution —
 * `targetId` is the specialist/service this row belongs to for
 * `PORTFOLIO`/`SERVICE_IMAGE` (`null` for salon-flat types), `displayOrder`
 * is this row's caller-controlled sort position within its group.
 */
@Serializable
data class MediaAssetResponseDto(
    val id: String,
    val salonId: String,
    val mediaType: String,
    val originalName: String,
    val mimeType: String,
    val fileSize: Long,
    val status: String,
    val url: String,
    val createdAt: String,
    val targetId: String? = null,
    val displayOrder: Int = 0,
)

/** `ReorderMediaRequest` (backend, `MediaController.reorder`, `PATCH /api/v1/salons/{salonId}/media/reorder`) — `mediaIds` must be exactly the current (mediaType, targetId) group's members, just permuted. */
@Serializable
data class ReorderMediaRequestDto(
    val mediaType: String,
    val targetId: String? = null,
    val mediaIds: List<String>,
)
