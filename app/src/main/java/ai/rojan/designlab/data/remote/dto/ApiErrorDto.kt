package ai.rojan.designlab.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire-format shape of every backend error response — see `ROJAN_Backend/API_CONTRACT.md`'s "Error format" section. */
@Serializable
data class ApiErrorDto(
    val timestamp: String,
    val status: Int,
    val error: String,
    /** Stable, machine-readable identifier (`GlobalExceptionHandler.errorCodeFor`), e.g. `"CUSTOMER_NOT_LINKED_TO_ACCOUNT"` — distinguishes exception types that share one HTTP status without parsing [message]. Was previously dropped by this DTO despite the backend always sending it (silently absorbed by `ignoreUnknownKeys`); added here because the Manager booking flow needs to react to specific error codes, not just status codes. */
    val errorCode: String? = null,
    val message: String,
    val path: String,
    val traceId: String? = null,
)
