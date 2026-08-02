package ai.rojan.designlab.domain.repository

/** Domain-facing shape of a paginated list response — mirrors `PagedResponseDto` without exposing it outside `data/remote`. */
data class PagedResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
