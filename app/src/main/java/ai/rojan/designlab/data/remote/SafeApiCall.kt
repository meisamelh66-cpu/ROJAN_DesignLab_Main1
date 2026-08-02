package ai.rojan.designlab.data.remote

import ai.rojan.designlab.data.remote.dto.ApiErrorDto
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** A non-2xx response the backend understood well enough to describe via its standard `ApiError` shape — see `ROJAN_Backend/API_CONTRACT.md`. */
class BackendApiException(
    val statusCode: Int,
    val apiError: ApiErrorDto?,
) : IOException(apiError?.message ?: "Backend request failed with status $statusCode")

/** No response reached the backend at all — offline, DNS failure, timeout, etc. Distinct from [BackendApiException] so callers can offer a "you're offline" state rather than a generic error. */
class NetworkUnavailableException(cause: Throwable) : IOException("No network connection", cause)

private val errorBodyJson = Json { ignoreUnknownKeys = true }

/**
 * Every repository's suspend network call should be wrapped in this rather
 * than a bare `runCatching`, so a non-2xx response consistently becomes a
 * [BackendApiException] carrying the decoded [ApiErrorDto] (when the body
 * parses) instead of an opaque [HttpException], and a connectivity failure
 * consistently becomes [NetworkUnavailableException] rather than a raw
 * [IOException] subtype callers would have to know to check for.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: HttpException) {
    val apiError = e.response()?.errorBody()?.let { body ->
        runCatching { errorBodyJson.decodeFromString<ApiErrorDto>(body.string()) }.getOrNull()
    }
    Result.failure(BackendApiException(e.code(), apiError))
} catch (e: IOException) {
    Result.failure(NetworkUnavailableException(e))
}
