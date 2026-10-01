package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Mirrors `ROJAN_Backend`'s `BannerResponse` (`GET /api/v1/public/banners`) —
 * kept 1:1 with the wire contract even though only [imageUrl] is currently
 * read by this app (see `BannerRepositoryImpl`).
 */
@Serializable
data class BannerResponseDto(
    val id: String,
    val target: String,
    val title: String? = null,
    val subtitle: String? = null,
    val href: String? = null,
    val imageUrl: String,
    val isActive: Boolean,
    val displayOrder: Int,
    val createdAt: String,
)
