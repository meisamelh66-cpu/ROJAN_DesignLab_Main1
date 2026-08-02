package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire-format shape of every paginated list endpoint — see `ROJAN_Backend/API_CONTRACT.md`'s "Pagination" section. */
@Serializable
data class PagedResponseDto<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
