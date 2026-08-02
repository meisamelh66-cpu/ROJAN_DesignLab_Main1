package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire-format shape of every backend error response — see `ROJAN_Backend/API_CONTRACT.md`'s "Error format" section. */
@Serializable
data class ApiErrorDto(
    val timestamp: String,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val traceId: String? = null,
)
