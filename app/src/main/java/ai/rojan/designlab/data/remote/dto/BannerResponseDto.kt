package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Mirrors `ROJAN_Backend`'s real `BannerResponse` DTO (`BannerController.kt`/
 * `PublicBannerController.kt`) exactly. Only [imageUrl] is guaranteed present
 * by the domain's own invariants (`Banner.create` requires a non-blank
 * storage key) — every other field is nullable with a safe default so an
 * evolving response shape can never throw `MissingFieldException`, same
 * defensive convention [PublicSalonSummaryResponseDto] already established
 * after an on-device crash from that exact failure mode.
 */
@Serializable
data class BannerResponseDto(
    val id: String? = null,
    val target: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val href: String? = null,
    val imageUrl: String,
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val createdAt: String? = null,
)
