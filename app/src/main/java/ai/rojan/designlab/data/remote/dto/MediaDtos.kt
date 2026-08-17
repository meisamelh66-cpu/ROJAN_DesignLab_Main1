package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire-format DTO for the ROJAN backend's `MediaAssetResponse` — see `ROJAN_Backend/api/.../media/MediaDtos.kt`. [url] is always a resolved, ready-to-render address (public CDN URL for LOGO/COVER/GALLERY today — never a signed document link, that's Document Archive's separate, permission-gated `access-url` endpoint). */
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
)
